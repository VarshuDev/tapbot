-- Cloudflare D1 Migration: 0011_align_manifest_versions.sql
-- Align legacy seeded D1 bot versions with actual bundled .botpkg manifest.json versions

UPDATE bots
SET current_version = '1.0.0'
WHERE id = 'bot_echo';

UPDATE bot_versions
SET version = '1.0.0'
WHERE bot_id = 'bot_echo' AND package_key = 'packages/bot_echo_1.0.0.botpkg';

UPDATE bots
SET current_version = '2.1.0'
WHERE id = 'bot_gemini';

UPDATE bot_versions
SET version = '2.1.0'
WHERE bot_id = 'bot_gemini' AND package_key = 'packages/bot_ai_assistant_2.1.0.botpkg';
