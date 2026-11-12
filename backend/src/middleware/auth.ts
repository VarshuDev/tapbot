/**
 * Admin Authentication Middleware
 */

import { Env, ApiResponse } from '../types';

async function timingSafeEqualStrings(a: string, b: string): Promise<boolean> {
    const encoder = new TextEncoder();
    const [hashA, hashB] = await Promise.all([
        crypto.subtle.digest('SHA-256', encoder.encode(a)),
        crypto.subtle.digest('SHA-256', encoder.encode(b))
    ]);
    const bytesA = new Uint8Array(hashA);
    const bytesB = new Uint8Array(hashB);
    let diff = 0;
    for (let i = 0; i < bytesA.length; i++) {
        diff |= bytesA[i] ^ bytesB[i];
    }
    return diff === 0;
}

export async function verifyAdminAuth(
    request: Request,
    env: Env
): Promise<{ authorized: boolean; errorResponse?: Response }> {
    const configuredKey = env.ADMIN_API_KEY?.trim();
    if (!configuredKey) {
        const payload: ApiResponse = {
            success: false,
            error: {
                code: 'SERVICE_UNAVAILABLE',
                message: 'Admin authentication is not configured on the server'
            }
        };

        return {
            authorized: false,
            errorResponse: new Response(JSON.stringify(payload), {
                status: 503,
                headers: {
                    'Content-Type': 'application/json'
                }
            })
        };
    }

    // Check Authorization: Bearer <key>
    const authHeader = request.headers.get('Authorization');
    let providedKey: string | null = null;

    if (authHeader && authHeader.startsWith('Bearer ')) {
        providedKey = authHeader.substring(7).trim();
    }

    // Fallback to X-Admin-Key header
    if (!providedKey) {
        providedKey = request.headers.get('X-Admin-Key')?.trim() || null;
    }

    if (!providedKey || !(await timingSafeEqualStrings(providedKey, configuredKey))) {
        const payload: ApiResponse = {
            success: false,
            error: {
                code: 'UNAUTHORIZED',
                message: 'Unauthorized: Valid admin API key required in Authorization or X-Admin-Key header'
            }
        };

        return {
            authorized: false,
            errorResponse: new Response(JSON.stringify(payload), {
                status: 401,
                headers: {
                    'Content-Type': 'application/json',
                    'WWW-Authenticate': 'Bearer realm="TapBot Admin API"'
                }
            })
        };
    }

    return { authorized: true };
}
