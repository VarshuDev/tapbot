-- Cloudflare D1 Migration: 0003_version_management.sql
-- Adds status (published/unpublished) and minimum_runtime_version to bot_versions

CREATE INDEX IF NOT EXISTS idx_bot_versions_status ON bot_versions(status);

