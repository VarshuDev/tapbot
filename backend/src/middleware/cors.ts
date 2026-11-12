/**
 * CORS Middleware for TapBot Catalog API
 */

import { Env } from '../types';

export const CORS_HEADERS = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Admin-Key',
    'Access-Control-Max-Age': '86400',
};

export const SECURITY_HEADERS = {
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'Strict-Transport-Security': 'max-age=31536000; includeSubDomains'
};

export function resolveCorsHeaders(request?: Request, env?: Env): Record<string, string> {
    if (!request) {
        return { ...CORS_HEADERS };
    }
    const url = new URL(request.url);
    if (!url.pathname.startsWith('/api/v1/admin')) {
        return {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type',
            'Access-Control-Max-Age': '86400',
        };
    }

    const requestOrigin = request.headers.get('Origin');
    const configuredOrigin = env?.ADMIN_ALLOWED_ORIGIN?.trim();
    const expectedOrigin = configuredOrigin || url.origin;
    const allowedOrigin = requestOrigin === expectedOrigin
        ? requestOrigin
        : (configuredOrigin ? null : url.origin);

    const headers: Record<string, string> = {
        'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Admin-Key, X-Package-Key, X-Filename',
        'Access-Control-Max-Age': '86400',
        'Vary': 'Origin',
    };
    if (allowedOrigin) {
        headers['Access-Control-Allow-Origin'] = allowedOrigin;
    }
    return headers;
}

export function handleCorsPreflight(request?: Request, env?: Env): Response {
    const headers = new Headers(resolveCorsHeaders(request, env));
    for (const [key, value] of Object.entries(SECURITY_HEADERS)) {
        headers.set(key, value);
    }
    return new Response(null, {
        status: 204,
        headers
    });
}

export function withCors(response: Response, request?: Request, env?: Env): Response {
    const headers = new Headers(response.headers);
    for (const [key, value] of Object.entries(resolveCorsHeaders(request, env))) {
        headers.set(key, value);
    }
    for (const [key, value] of Object.entries(SECURITY_HEADERS)) {
        if (!headers.has(key)) {
            headers.set(key, value);
        }
    }
    return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers
    });
}
