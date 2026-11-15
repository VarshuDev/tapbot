-- Cloudflare D1 Migration: 0010_fix_gemini_legacy_package.sql
-- Point legacy seeded bot_gemini v2.0.0 version row at real bundled Gemini .botpkg archive and verified SHA-256

UPDATE bot_versions
SET package_key = 'packages/bot_ai_assistant_2.1.0.botpkg',
    package_size = 1212,
    sha256 = '44c098853cd5d46484141cefa70e3bbb763915e30c71848bd67316cb8ff9c185'
WHERE bot_id = 'bot_gemini' AND version = '2.0.0';
