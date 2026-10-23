-- Cloudflare D1 Migration: 0006_commander_bot.sql
-- Seed TapBot Commander flagship bot

-- 1. Insert TapBot Commander
INSERT OR IGNORE INTO bots (
    id, slug, name, description, long_description, icon_url, category, runtime, current_version, status, downloads_count, rating, created_at, updated_at
) VALUES (
    'bot_commander_flagship',
    'tapbot-commander',
    'TapBot Commander',
    'Flagship on-device remote controller, device watchdog, and fleet manager for your Android phone.',
    'The premier flagship on-device Telegram bot for TapBot. Runs natively on Android ART to provide real-time hardware telemetry (battery, RAM, disk, network, uptime), bot fleet management (/bots, /startbot, /stopbot, /restartbot, /logs), remote phone controls (ring phone, flashlight, notifications, clipboard, vibrate), APK delivery directly in chat with SHA-256 integrity verification, and autonomous watchdog alerts for low battery and bot crashes.',
    'https://images.unsplash.com/photo-1518770660439-4636190af475?w=200',
    'automation',
    'native_art',
    '1.0.0',
    'published',
    150,
    5.0,
    '2026-10-22T00:00:00Z',
    '2026-10-22T00:00:00Z'
);

-- 2. Insert TapBot Commander Initial Version 1.0.0
INSERT OR IGNORE INTO bot_versions (
    id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at
) VALUES (
    'ver_commander_100',
    'bot_commander_flagship',
    '1.0.0',
    'packages/tapbot-commander_1.0.0.botpkg',
    1703,
    '51dd47dec8e9bbc0c88b094887c1a05da4c6835c2894f350986e25684edeb7f3',
    'Flagship release of TapBot Commander: Remote Android device telemetry, bot fleet control, APK delivery in chat, and autonomous watchdog alerts.',
    1,
    '1.0.0',
    'published',
    '2026-10-22T00:00:00Z'
);

-- 3. Insert TapBot Commander Credentials
INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type) VALUES
('cred_cmd_token', 'bot_commander_flagship', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_cmd_owner_id', 'bot_commander_flagship', 'owner_id', 'Bot Owner User ID', 'Numeric Telegram user ID from @userinfobot to authorize sensitive device commands (e.g. 123456789)', 0, 0, 'text'),
('cred_cmd_battery_thresh', 'bot_commander_flagship', 'low_battery_threshold', 'Low Battery Threshold (%)', 'Battery percentage that triggers an urgent Telegram alert (default 15)', 0, 0, 'number');
