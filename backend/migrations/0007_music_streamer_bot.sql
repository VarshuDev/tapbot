-- Cloudflare D1 Migration: 0007_music_streamer_bot.sql
-- Seed TapStream Music Bot (On-Device Music Streaming & Audio Telegram Bot)

-- 1. Insert TapStream Music Bot
INSERT OR IGNORE INTO bots (
    id, slug, name, description, long_description, icon_url, category, runtime, current_version, status, downloads_count, rating, created_at, updated_at
) VALUES (
    'bot_music_streamer',
    'tapstream-music',
    'TapStream Music Bot',
    'On-device music streaming, YouTube/yt-dlp resolution, 24/7 internet radio stations, and Android speaker playback.',
    'TapStream is an on-device music streaming and audio distribution bot for Telegram running natively on Android ART. Features dual audio output: streams music out loud through your phone speaker or Bluetooth audio system, and delivers playable audio tracks directly into Telegram chats via sendAudio. Integrates yt-dlp API resolution, direct audio stream playback (.mp3, .m4a, .aac, .ogg, .opus), 6 curated 24/7 internet radio stations, interactive queue management, dynamic ASCII now-playing HUD, and hardware volume control.',
    'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200',
    'media',
    'native_art',
    '1.0.0',
    'published',
    210,
    5.0,
    '2026-11-04T00:00:00Z',
    '2026-11-04T00:00:00Z'
);

-- 2. Insert TapStream Music Bot Initial Version 1.0.0
INSERT OR IGNORE INTO bot_versions (
    id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at
) VALUES (
    'ver_streamer_100',
    'bot_music_streamer',
    '1.0.0',
    'packages/tapstream-music-1.0.0.botpkg',
    1827,
    '178434b2d9ff07199455a3ae04373be2d13bec341e398baa99529c4635550b0a',
    'Initial release of TapStream Music Bot: YouTube/yt-dlp stream resolver, 24/7 internet radio stations, dual phone speaker and in-chat audio delivery, and interactive queue controls.',
    1,
    '1.0.0',
    'published',
    '2026-11-04T00:00:00Z'
);

-- 3. Insert TapStream Music Bot Credentials
INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type) VALUES
('cred_streamer_token', 'bot_music_streamer', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_streamer_resolver', 'bot_music_streamer', 'stream_resolver_url', 'Custom yt-dlp Resolver API URL', 'Optional custom yt-dlp, Cobalt, or Piped resolver endpoint', 0, 0, 'text'),
('cred_streamer_vol', 'bot_music_streamer', 'default_volume', 'Default Speaker Volume (%)', 'Initial Android phone speaker volume from 0 to 100 (default 80)', 0, 0, 'number'),
('cred_streamer_mode', 'bot_music_streamer', 'default_playback_mode', 'Default Playback Mode', 'both (speaker + chat), speaker (phone only), or chat (telegram only)', 0, 0, 'text');
