-- Cloudflare D1 Migration: 0005_exon_and_metrics.sql
-- Add analytics metrics and seed Exon Robot

-- 1. Add metrics columns to bots table
ALTER TABLE bots ADD COLUMN downloads_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bots ADD COLUMN rating REAL NOT NULL DEFAULT 5.0;

-- 2. Indexes for metrics
CREATE INDEX IF NOT EXISTS idx_bots_downloads ON bots(downloads_count DESC);
CREATE INDEX IF NOT EXISTS idx_bots_rating ON bots(rating DESC);

-- 3. Insert or Replace Exon Robot
INSERT OR IGNORE INTO bots (
    id, slug, name, description, long_description, icon_url, category, runtime, current_version, status, downloads_count, rating, created_at, updated_at
) VALUES (
    'bot_1790084493920_n4l8lj',
    'exon-robot',
    'Exon Robot',
    'Advanced Telegram group management bot with ban, mute, pin, and moderation capabilities.',
    'High-performance Telegram group management bot inspired by ExonRobot. Runs directly on your Android device via ART runtime with MTProto and Bot API support.',
    'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200',
    'automation',
    'native_art',
    '1.0.0',
    'published',
    42,
    5.0,
    '2026-09-22T00:00:00Z',
    '2026-10-10T00:00:00Z'
);

-- 4. Insert Exon Robot Initial Version 1.0.0
INSERT OR IGNORE INTO bot_versions (
    id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at
) VALUES (
    'ver_exon_100',
    'bot_1790084493920_n4l8lj',
    '1.0.0',
    'packages/bot_exon_1.0.0.botpkg',
    614,
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    'Initial release of Exon Robot for TapBot on-device runner with MTProto & Bot API support.',
    1,
    '1.0.0',
    'published',
    '2026-09-22T00:00:00Z'
);

-- 5. Insert Exon Robot Credentials
INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type) VALUES
('cred_exon_token', 'bot_1790084493920_n4l8lj', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_exon_api_id', 'bot_1790084493920_n4l8lj', 'app_api_id', 'Telegram App API ID', 'Your Telegram App API ID obtained from my.telegram.org', 1, 0, 'text'),
('cred_exon_api_hash', 'bot_1790084493920_n4l8lj', 'app_api_hash', 'Telegram App API Hash', 'Your Telegram App API Hash obtained from my.telegram.org', 1, 1, 'password'),
('cred_exon_owner_id', 'bot_1790084493920_n4l8lj', 'owner_id', 'Bot Owner User ID', 'Numeric Telegram user ID of the bot master / owner (e.g. 7342044274)', 1, 0, 'text');
