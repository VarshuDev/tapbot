/**
 * Object Storage Abstraction (R2 with automatic Cloudflare D1 fallback)
 *
 * Uses Cloudflare R2 when `env.BUCKET` is bound; otherwise persists small
 * package archives and icons in D1's `storage_objects` table so deployments
 * run cleanly on Cloudflare's free Workers + D1 tier without R2 enabled.
 */

import { Env } from '../types';

export const MAX_D1_OBJECT_BYTES = 1_500_000;

export interface StoredObjectResult {
    body: ReadableStream | Uint8Array | ArrayBuffer;
    contentType: string;
    httpEtag: string;
    writeHttpMetadata: (headers: Headers) => void;
}

interface StorageObjectRow {
    key: string;
    content_type: string;
    size: number;
    data_base64: string;
    etag: string;
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    const chunkSize = 0x8000;
    let binary = '';
    for (let i = 0; i < bytes.length; i += chunkSize) {
        const slice = bytes.subarray(i, Math.min(i + chunkSize, bytes.length));
        binary += String.fromCharCode(...slice);
    }
    return btoa(binary);
}

function base64ToUint8Array(base64: string): Uint8Array {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

async function computeEtag(buffer: ArrayBuffer): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', buffer);
    const hex = Array.from(new Uint8Array(digest))
        .slice(0, 16)
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');
    return `"${hex}"`;
}

export async function putStoredObject(
    env: Env,
    key: string,
    data: ArrayBuffer,
    contentType: string
): Promise<{ size: number; etag: string }> {
    if (env.BUCKET) {
        const result = await env.BUCKET.put(key, data, {
            httpMetadata: { contentType }
        });
        return {
            size: result?.size ?? data.byteLength,
            etag: result?.etag ?? (await computeEtag(data))
        };
    }

    if (data.byteLength > MAX_D1_OBJECT_BYTES) {
        throw new Error(
            `Object exceeds D1 fallback size limit (${data.byteLength} > ${MAX_D1_OBJECT_BYTES} bytes)`
        );
    }

    const base64 = arrayBufferToBase64(data);
    const etag = await computeEtag(data);
    const now = new Date().toISOString();

    await env.DB.prepare(
        `INSERT INTO storage_objects (key, content_type, size, data_base64, etag, created_at)
         VALUES (?, ?, ?, ?, ?, ?)
         ON CONFLICT(key) DO UPDATE SET
            content_type = excluded.content_type,
            size = excluded.size,
            data_base64 = excluded.data_base64,
            etag = excluded.etag,
            created_at = excluded.created_at`
    )
        .bind(key, contentType, data.byteLength, base64, etag, now)
        .run();

    return {
        size: data.byteLength,
        etag
    };
}

export async function getStoredObject(
    env: Env,
    key: string
): Promise<StoredObjectResult | null> {
    if (env.BUCKET) {
        const object = await env.BUCKET.get(key);
        if (!object) return null;
        return {
            body: object.body,
            contentType: object.httpMetadata?.contentType || 'application/octet-stream',
            httpEtag: object.httpEtag,
            writeHttpMetadata: (headers: Headers) => object.writeHttpMetadata(headers)
        };
    }

    const row = await env.DB.prepare(
        'SELECT key, content_type, size, data_base64, etag FROM storage_objects WHERE key = ?'
    )
        .bind(key)
        .first<StorageObjectRow>();

    if (!row) return null;

    const bytes = base64ToUint8Array(row.data_base64);
    const contentType = row.content_type || 'application/octet-stream';

    return {
        body: bytes,
        contentType,
        httpEtag: row.etag,
        writeHttpMetadata: (headers: Headers) => {
            headers.set('Content-Type', contentType);
        }
    };
}
