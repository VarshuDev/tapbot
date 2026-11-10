-- Cloudflare D1 Migration: 0008_v230_catalog_sync.sql
-- TapBot v2.3.0: Sync bundled .botpkg checksums/sizes, Custom category, and v2.3.0 catalog entries

-- 1. Add Custom Workflow category for no-code user-built bots
INSERT OR IGNORE INTO categories (id, name, slug) VALUES
('cat_custom', 'Custom', 'custom');

-- 2. Sync existing flagship and core bots with v2.3.0 capabilities
UPDATE bots
SET long_description = 'The premier flagship on-device Telegram bot for TapBot v2.3.0. Runs natively on Android ART to provide real-time hardware telemetry (/status, /battery, /ram, /disk, /wifi, /location), TTS speech (/tts), scheduled alerts (/schedule), inline HUD controls, bot fleet management (/bots, /startbot, /stopbot, /restartbot, /logs), remote phone controls, APK delivery with SHA-256 verification, and autonomous crash/battery watchdog alerts.',
    updated_at = '2026-11-10T00:00:00Z'
WHERE id IN ('bot_commander_flagship', 'bot_commander');

UPDATE bots
SET long_description = 'TapStream v2.3.0 is an on-device music streaming and audio distribution bot for Telegram running natively on Android ART with Android 14/15 mediaPlayback foreground compliance and AudioFocus management. Supports dual phone speaker and in-chat playback, YouTube/yt-dlp resolution, 6 curated internet radio stations, /shuffle, /loop, /seek, /remove, and interactive inline player controls.',
    updated_at = '2026-11-10T00:00:00Z'
WHERE id = 'bot_music_streamer';

UPDATE bots
SET long_description = 'High-performance Telegram group management bot inspired by ExonRobot. Upgraded in TapBot v2.3.0 with persistent JSON moderation store, timed /tmute and /tban, /promote and /demote, welcome greetings (/setwelcome, /welcome, /clearwelcome), antiflood protection, active /lock message deletion, and interactive inline menus.',
    updated_at = '2026-11-10T00:00:00Z'
WHERE id IN ('bot_1790084493920_n4l8lj', 'bot_exon');

-- 3. Upsert catalog bots present in offline MockCatalogApi that were missing or outdated in D1
INSERT OR IGNORE INTO bots (
    id, slug, name, description, long_description, icon_url, category, runtime, current_version, status, downloads_count, rating, created_at, updated_at
) VALUES
(
    'bot_echo',
    'echo-assistant-bot',
    'Echo Assistant Bot',
    'Echoes incoming messages with latency metadata and system diagnostics.',
    'Echo Assistant Bot is designed to test background service execution on Android ART. Echoes any incoming message back to the sender with battery telemetry and processing latency.',
    'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200',
    'utilities',
    'built-in',
    '1.0.0',
    'published',
    90,
    4.7,
    '2026-09-19T01:00:00Z',
    '2026-11-10T00:00:00Z'
),
(
    'bot_rss_notifier',
    'rss-notifier-broadcaster',
    'RSS Channel Broadcaster',
    'Periodically checks your favorite RSS/Atom feeds and broadcasts new articles to your Telegram channel.',
    'Runs on your Android device 24/7 with full RSS 2.0 and Atom 1.0 feed parsing, persisted seen-GUID deduplication across restarts, and interactive /feed, /check, and /clearcache commands.',
    'https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200',
    'media',
    'built-in',
    '1.2.0',
    'published',
    95,
    4.9,
    '2026-09-19T02:00:00Z',
    '2026-11-10T00:00:00Z'
),
(
    'bot_ai_assistant',
    'gemini-assistant-v2',
    'Gemini Smart Assistant',
    'Conversational AI assistant powered by Google Gemini running directly through your personal Telegram account.',
    'Turns your personal Telegram bot into a generative AI assistant with live Google Gemini API integration, per-chat conversation history, /ask, /summarize, /code, /model, /system, /clear, and automatic >4096 character chunking.',
    'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=200',
    'productivity',
    'built-in',
    '2.1.0',
    'published',
    180,
    5.0,
    '2026-10-01T00:00:00Z',
    '2026-11-10T00:00:00Z'
),
(
    'bot_music',
    'music-bot',
    'Music Bot',
    'Streams audio, queues playlists, and provides music playback controls.',
    'Runs locally on Android to manage Telegram audio playback queues, AudioFocus, /shuffle, /loop, /seek, and inline playback buttons.',
    'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200',
    'media',
    'native_art',
    '1.4.0',
    'published',
    110,
    4.8,
    '2026-09-01T10:00:00Z',
    '2026-11-10T00:00:00Z'
),
(
    'bot_ai',
    'ai-bot',
    'AI Bot',
    'Generative AI conversational assistant powered on-device.',
    'Provides multi-turn Gemini AI reasoning, text summarization, and code explanation directly through your personal Telegram bot.',
    'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=200',
    'productivity',
    'native_art',
    '1.2.0',
    'published',
    105,
    4.8,
    '2026-09-01T10:00:00Z',
    '2026-11-10T00:00:00Z'
),
(
    'bot_utility',
    'utility-bot',
    'Utility Bot',
    'System diagnostics, uptime monitoring, latency checks, and test crash simulation.',
    'Monitors Android ART execution metrics, memory usage, battery thermal stats, and allows resilience testing via /crash.',
    'https://images.unsplash.com/photo-1518770660439-4636190af475?w=200',
    'utilities',
    'native_art',
    '1.1.0',
    'published',
    85,
    4.7,
    '2026-09-01T10:00:00Z',
    '2026-11-10T00:00:00Z'
);

-- 4. Synchronize bot_versions SHA-256 and package_size values with bundled .botpkg archives
UPDATE bot_versions
SET package_size = 1213,
    sha256 = 'e269c5351b048201ad1b7e417e954c47a2a745e0d8522b68ff2de03a581d65d0',
    release_notes = 'Exon Robot v1.0.0: Timed moderation (/tmute, /tban), persisted JSON rules/locks/antiflood, welcome greetings, and inline controls.'
WHERE bot_id IN ('bot_1790084493920_n4l8lj', 'bot_exon') AND version = '1.0.0';

UPDATE bot_versions
SET package_key = 'packages/bot_echo_1.0.0.botpkg',
    package_size = 1113,
    sha256 = 'be89e34e178e8be6643a5e7dc0416d7309f43cb140f627080b33f1002f23cbff'
WHERE bot_id = 'bot_echo';

UPDATE bot_versions
SET package_size = 1248,
    sha256 = '91f367ada04dae04ff3536a8a334d0b18aa411fb51faded21d17ce775663015b'
WHERE bot_id IN ('bot_rss', 'bot_rss_notifier') AND version = '1.2.0';

UPDATE bot_versions
SET package_size = 1114,
    sha256 = 'f3de200f9b4903d4519556940b8a3f9f610eb5146d5105f0b0e32fd48c0e5b70'
WHERE bot_id = 'bot_ping_pong' AND version = '1.0.0';

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_echo_100', 'bot_echo', '1.0.0', 'packages/bot_echo_1.0.0.botpkg', 1113, 'be89e34e178e8be6643a5e7dc0416d7309f43cb140f627080b33f1002f23cbff', 'Initial release of Echo Assistant Bot with structured latency diagnostics.', 1, '1.0.0', 'published', '2026-09-19T01:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_echo');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_rss_notifier_120', 'bot_rss_notifier', '1.2.0', 'packages/bot_rss_1.2.0.botpkg', 1248, '91f367ada04dae04ff3536a8a334d0b18aa411fb51faded21d17ce775663015b', 'RSS 2.0 & Atom 1.0 parser with restart deduplication and /feed management.', 1, '1.0.0', 'published', '2026-09-19T02:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_rss_notifier');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_ai_assistant_210', 'bot_ai_assistant', '2.1.0', 'packages/bot_ai_assistant_2.1.0.botpkg', 1212, '44c098853cd5d46484141cefa70e3bbb763915e30c71848bd67316cb8ff9c185', 'Live Google Gemini API integration with per-chat history, /ask, /summarize, /code, /model, /system, and 4096-char chunking.', 1, '1.0.0', 'published', '2026-10-01T00:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_ai_assistant');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_music_140', 'bot_music', '1.4.0', 'packages/bot_music_1.4.0.botpkg', 1189, 'c58161d93bdefe662e41d1c63f5dde7b1343478a9a90d231bd39d20d1d28360a', 'High-resolution audio streaming with AudioFocus, /shuffle, /loop, /seek, and inline controls.', 1, '1.0.0', 'published', '2026-09-20T10:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_music');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_music_100', 'bot_music', '1.0.0', 'packages/bot_music_1.0.0.botpkg', 1139, '8c12f7ae7b3134dc5a27ee7c8c0b5396f6101fe1b97b26d61da498d926f7b391', 'Initial release of Music Bot for Android.', 1, '1.0.0', 'published', '2026-09-01T10:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_music');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_ai_120', 'bot_ai', '1.2.0', 'packages/bot_ai_1.2.0.botpkg', 1167, '564fad24f79189a8f9409ad950964eae9b28bbd4a7384fa1a52df3aa5f3cc5f4', 'Gemini 2.5 Flash support with multi-turn memory, /summarize, and /code.', 1, '1.0.0', 'published', '2026-09-18T12:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_ai');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_ai_100', 'bot_ai', '1.0.0', 'packages/bot_ai_1.0.0.botpkg', 1139, 'f8f130c0af3050937663f80df9a09766aa743dc7a4a99444d25abe4246913b0e', 'Initial release of AI Assistant Bot.', 1, '1.0.0', 'published', '2026-09-01T10:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_ai');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_util_110', 'bot_utility', '1.1.0', 'packages/bot_utility_1.1.0.botpkg', 1101, '1b4c04c33aec952bca1809257ca0c3dbbc9b588471373264299b0dce49134afe', 'Battery thermal reporting and thread pool introspection.', 1, '1.0.0', 'published', '2026-09-15T15:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_utility');

INSERT OR IGNORE INTO bot_versions (id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at)
SELECT 'ver_util_100', 'bot_utility', '1.0.0', 'packages/bot_utility_1.0.0.botpkg', 1104, '18207df14e1e7d811cf672f930e5a7286dedd331653b47224e8da4cca6f1663b', 'Initial release of Utility Bot.', 1, '1.0.0', 'published', '2026-09-01T10:00:00Z'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_utility');

-- 5. Ensure credential specs exist for synced bots
INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_echo_token', 'bot_echo', 'bot_token', 'Telegram Bot Token', 'Token obtained from @BotFather in Telegram.', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_echo');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_rss_not_token', 'bot_rss_notifier', 'bot_token', 'Telegram Bot Token', 'Token obtained from @BotFather in Telegram.', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_rss_notifier');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_rss_not_chat', 'bot_rss_notifier', 'target_chat_id', 'Target Chat or Channel ID', 'Channel or Group ID (e.g. -1001234567890).', 1, 0, 'text'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_rss_notifier');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_rss_not_url', 'bot_rss_notifier', 'feed_url', 'RSS / Atom Feed URL', 'HTTP/HTTPS URL of the RSS feed to monitor.', 1, 0, 'url'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_rss_notifier');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_ai_ast_token', 'bot_ai_assistant', 'bot_token', 'Telegram Bot Token', 'Token obtained from @BotFather in Telegram.', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_ai_assistant');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_ai_ast_key', 'bot_ai_assistant', 'gemini_api_key', 'Gemini API Key', 'Obtain a free Google AI Studio API key at https://aistudio.google.com', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_ai_assistant');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_music_token', 'bot_music', 'bot_token', 'Telegram Bot Token', 'Token obtained from @BotFather in Telegram.', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_music');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_ai_token', 'bot_ai', 'bot_token', 'Telegram Bot Token', 'Token obtained from @BotFather in Telegram.', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_ai');

INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type)
SELECT 'cred_util_token', 'bot_utility', 'bot_token', 'Telegram Bot Token', 'Token obtained from @BotFather in Telegram.', 1, 1, 'password'
WHERE EXISTS (SELECT 1 FROM bots WHERE id = 'bot_utility');
