-- Cloudflare D1 Migration: 0009_d1_storage_objects.sql
-- Binary object fallback table for Cloudflare Workers + D1 deployments without R2 enabled

CREATE TABLE IF NOT EXISTS storage_objects (
    key TEXT PRIMARY KEY,
    content_type TEXT NOT NULL DEFAULT 'application/octet-stream',
    size INTEGER NOT NULL,
    data_base64 TEXT NOT NULL,
    etag TEXT NOT NULL,
    created_at TEXT NOT NULL
);
