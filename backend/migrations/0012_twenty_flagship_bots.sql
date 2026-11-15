-- Cloudflare D1 Migration: 0012_twenty_flagship_bots.sql
-- TapBot 20 Daily Flagship Bots Expansion: Categories, Bots, Versions, and Credentials

-- 1. Insert New Categories (Lifestyle, Finance, Productivity, Developer Tools, Entertainment, Social)
INSERT OR IGNORE INTO categories (id, name, slug) VALUES
('cat_lifestyle', 'Lifestyle', 'lifestyle'),
('cat_finance', 'Finance', 'finance'),
('cat_productivity', 'Productivity', 'productivity'),
('cat_devtools', 'Developer Tools', 'devtools'),
('cat_entertainment', 'Entertainment', 'entertainment'),
('cat_social', 'Social', 'social');

-- 2. Update existing flagship bots with expanded ratings, download statistics, and descriptions
UPDATE bots
SET name = 'TapBot Commander',
    description = 'Flagship on-device remote controller, device watchdog, and fleet manager for your Android phone.',
    long_description = 'The premier flagship on-device Telegram bot for TapBot. Runs natively on Android ART to provide real-time hardware telemetry (battery, RAM, disk, network, uptime), bot fleet management (/bots, /startbot, /stopbot, /restartbot, /logs), remote phone controls (ring phone, flashlight, notifications, clipboard, vibrate), APK delivery directly in chat with SHA-256 integrity verification, and autonomous watchdog alerts for low battery and bot crashes.',
    category = 'automation',
    current_version = '1.0.0',
    downloads_count = 52000,
    rating = 5.0,
    updated_at = '2026-11-15T00:00:00Z'
WHERE id IN ('bot_commander', 'bot_commander_flagship');

UPDATE bots
SET name = 'TapStream Music Bot',
    description = 'On-device music streaming, YouTube/yt-dlp resolution, 24/7 internet radio stations, and Android speaker playback.',
    long_description = 'TapStream is an on-device music streaming and audio distribution bot for Telegram running natively on Android ART. Features dual audio output: streams music out loud through your phone speaker or Bluetooth audio system, and delivers playable audio tracks directly into Telegram chats via sendAudio. Integrates yt-dlp API resolution, direct audio stream playback (.mp3, .m4a, .aac, .ogg, .opus), 6 curated 24/7 internet radio stations, interactive queue management, dynamic ASCII now-playing HUD, and hardware volume control.',
    category = 'media',
    current_version = '1.0.0',
    downloads_count = 28000,
    rating = 4.9,
    updated_at = '2026-11-15T00:00:00Z'
WHERE id = 'bot_music_streamer';

UPDATE bots
SET name = 'Exon Robot Sentinel',
    description = 'Advanced Telegram group management bot with ban, mute, pin, and moderation capabilities.',
    long_description = 'High-performance Telegram group management bot inspired by ExonRobot. Runs directly on your Android device via ART runtime with MTProto, timed moderation, persisted locks & antiflood, welcome greetings, and inline controls.',
    category = 'automation',
    current_version = '1.0.0',
    downloads_count = 36000,
    rating = 4.9,
    updated_at = '2026-11-15T00:00:00Z'
WHERE id IN ('bot_exon', 'bot_1790084493920_n4l8lj');

UPDATE bots
SET name = 'Gemini Smart Assistant',
    description = 'Conversational AI assistant powered by Google Gemini running directly through your personal Telegram account.',
    long_description = 'Turns your personal Telegram bot into a generative AI assistant with live Google Gemini API integration, per-chat conversation history, /ask, /summarize, /code, /model, /system, /clear, and automatic >4096 character chunking.',
    category = 'productivity',
    current_version = '2.1.0',
    downloads_count = 62000,
    rating = 5.0,
    updated_at = '2026-11-15T00:00:00Z'
WHERE id IN ('bot_ai_assistant', 'bot_gemini');

UPDATE bots
SET name = 'NewsPulse RSS Broadcaster',
    description = 'Periodically checks your favorite RSS/Atom feeds and broadcasts new articles to your Telegram channel.',
    long_description = 'Runs on your Android device 24/7 with full RSS 2.0 and Atom 1.0 feed parsing, persisted seen-GUID deduplication across restarts, and interactive /feed, /check, and /clearcache commands.',
    category = 'media',
    current_version = '1.2.0',
    downloads_count = 15200,
    rating = 4.9,
    updated_at = '2026-11-15T00:00:00Z'
WHERE id IN ('bot_rss_notifier', 'bot_rss');

-- Ensure canonical bot_commander row exists if not already seeded under this ID
INSERT OR IGNORE INTO bots (
    id, slug, name, description, long_description, icon_url, category, runtime, current_version, status, downloads_count, rating, created_at, updated_at
) VALUES (
    'bot_commander',
    'tapbot-commander-primary',
    'TapBot Commander',
    'Flagship on-device remote controller, device watchdog, and fleet manager for your Android phone.',
    'The premier flagship on-device Telegram bot for TapBot. Runs natively on Android ART to provide real-time hardware telemetry (battery, RAM, disk, network, uptime), bot fleet management (/bots, /startbot, /stopbot, /restartbot, /logs), remote phone controls (ring phone, flashlight, notifications, clipboard, vibrate), APK delivery directly in chat with SHA-256 integrity verification, and autonomous watchdog alerts for low battery and bot crashes.',
    'https://images.unsplash.com/photo-1518770660439-4636190af475?w=200',
    'automation',
    'native_art',
    '1.0.0',
    'published',
    52000,
    5.0,
    '2026-10-22T00:00:00Z',
    '2026-11-15T00:00:00Z'
);

-- 3. Insert the 15 New Daily Flagship Bots
INSERT OR IGNORE INTO bots (
    id, slug, name, description, long_description, icon_url, category, runtime, current_version, status, downloads_count, rating, created_at, updated_at
) VALUES
(
    'bot_habit_forge',
    'habit-forge',
    'HabitForge Daily Tracker',
    'Daily habit check-ins, streak preservation, and visual progress tracking.',
    'Build lasting habits with daily Telegram check-ins, automated evening reviews, streak tracking, and interactive inline buttons. Runs entirely on your Android device with zero cloud storage.',
    'https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=200',
    'lifestyle',
    'native_art',
    '1.0.0',
    'published',
    19500,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_penny_wise',
    'penny-wise',
    'PennyWise Expense Tracker',
    '3-second expense logging, daily spend limits, and monthly budget analytics.',
    'Fast, friction-free personal finance tracker. Log expenses in 3 seconds directly via Telegram chat commands, track spending categories, visualize monthly budget progress, and export CSV ledgers.',
    'https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=200',
    'finance',
    'native_art',
    '1.0.0',
    'published',
    23100,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_pomo_focus',
    'pomo-focus',
    'PomoFocus Deep Work Timer',
    '25/5 Pomodoro intervals, phone haptic alerts, and task sprint logs.',
    'On-device deep work companion with classic 25/5 interval cycles, short/long break sequences, haptic feedback triggers, sprint statistics, and task tagging.',
    'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=200',
    'productivity',
    'native_art',
    '1.0.0',
    'published',
    14200,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_tap_note',
    'tap-note',
    'TapNote Scratchpad',
    'Rapid thought capture, todo lists, daily micro-journal, and markdown export.',
    'Lightweight scratchpad for capturing fleeting ideas, maintaining active todo checklists, logging micro-journal reflections, and exporting clean markdown notes directly in chat.',
    'https://images.unsplash.com/photo-1517842645767-c639042777db?w=200',
    'productivity',
    'native_art',
    '1.0.0',
    'published',
    17000,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_sky_pulse',
    'sky-pulse',
    'SkyPulse Weather Radar',
    'Morning 7 AM weather briefing, rain radar, UV, and severe weather alerts.',
    'Real-time atmospheric monitoring bot providing instant temperature, humidity, wind conditions, multi-day forecasts, severe weather warnings, and scheduled 7:00 AM morning briefings.',
    'https://images.unsplash.com/photo-1534088568595-a066f410bcda?w=200',
    'utilities',
    'native_art',
    '1.0.0',
    'published',
    31500,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_remind_me',
    'remind-me',
    'RemindMe Health & Pill Alarms',
    'Natural language reminders, water alarms, medication schedules, and snooze.',
    'Reliable on-device alarm bot for scheduling natural language reminders, recurring medication doses, periodic hydration pings, and interactive snooze buttons.',
    'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=200',
    'lifestyle',
    'native_art',
    '1.0.0',
    'published',
    21000,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_link_drop',
    'link-drop',
    'LinkDrop Link & Clipboard Vault',
    'Save articles/URLs from PC to phone, tag bookmarks, and sync phone clipboard.',
    'Cross-device bookmarking vault. Drop links from your desktop or phone into Telegram, organize with topic tags, search full URLs, and copy saved snippets directly to your Android clipboard.',
    'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=200',
    'utilities',
    'native_art',
    '1.0.0',
    'published',
    11400,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_poly_glot',
    'poly-glot',
    'PolyGlot Instant Translator',
    'Instant multi-language translation in chat, auto-detect, and word of the day.',
    'Pocket translator supporting over 50 languages. Translate phrases instantly with ISO language codes, detect source languages with confidence scoring, and practice daily vocabulary flashcards.',
    'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=200',
    'utilities',
    'native_art',
    '1.0.0',
    'published',
    15800,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_coin_radar',
    'coin-radar',
    'CoinRadar Crypto & Stock Watcher',
    'Live asset prices, portfolio P&L, 24h market trends, and price trigger alerts.',
    'Financial market watcher delivering real-time quotes, 7-day ASCII sparkline trend charts, personal portfolio valuation, top crypto market rankings, and customizable price trigger notifications.',
    'https://images.unsplash.com/photo-1621416894569-0f39ed31d247?w=200',
    'finance',
    'native_art',
    '1.0.0',
    'published',
    39000,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_fit_track',
    'fit-track',
    'FitTrack Hydration & Workouts',
    'Daily water intake, workout activity logger, calorie burn, and target rings.',
    'Personal fitness companion for logging workouts across multiple disciplines (running, gym, cycling, yoga), tracking daily water intake, monitoring active calorie burn, and viewing weekly stats.',
    'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=200',
    'lifestyle',
    'native_art',
    '1.0.0',
    'published',
    17500,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_parcel_track',
    'parcel-track',
    'ParcelTrack Delivery Watcher',
    'Multi-carrier package tracking (FedEx, UPS, DHL, Amazon) and delivery alerts.',
    'Shipment tracking hub supporting global couriers. Add parcel tracking numbers with custom labels, monitor milestone progress timelines, and receive alerts upon delivery completion.',
    'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=200',
    'utilities',
    'native_art',
    '1.0.0',
    'published',
    13800,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_git_notifier',
    'git-notifier',
    'GitNotifier Repo Watcher',
    'GitHub & GitLab event alerts, issues, PR reviews, and CI/CD build failures.',
    'Developer repository sentinel. Monitor public or private GitHub and GitLab repositories for incoming commits, pull request review requests, issue discussions, and CI/CD workflow pass/fail statuses.',
    'https://images.unsplash.com/photo-1618401471353-b98afee0b2eb?w=200',
    'devtools',
    'native_art',
    '1.0.0',
    'published',
    27000,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_cam_sentry',
    'cam-sentry',
    'CamSentry Hardware Security',
    'Remote camera photo capture, flash photo, motion alert, and battery guard.',
    'Hardware surveillance tool leveraging your Android phone camera. Trigger remote photo snapshots from front or back lenses, capture flash photos in darkness, and receive hardware telemetry reports.',
    'https://images.unsplash.com/photo-1557597774-9d273605dfa9?w=200',
    'automation',
    'native_art',
    '1.0.0',
    'published',
    34500,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_whisper_box',
    'whisper-box',
    'WhisperBox Anonymous Mailbox',
    'Shareable anonymous question/feedback inbox for creators and group members.',
    'Private anonymous feedback inbox. Generate unique question links, receive unread anonymous inquiries safely, reply anonymously back to senders, and manage moderation blocks.',
    'https://images.unsplash.com/photo-1577563908411-5077b6dc7624?w=200',
    'social',
    'native_art',
    '1.0.0',
    'published',
    21500,
    4.8,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
),
(
    'bot_trivia_master',
    'trivia-master',
    'TriviaMaster Daily Brain Quiz',
    'Daily 5-question trivia, interactive inline polls, and learning streak counter.',
    'Interactive daily knowledge game. Engage in 5-question multi-topic quizzes (Science, History, Technology, Geography), tackle the Daily Challenge question, maintain streaks, and climb score leaderboards.',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?w=200',
    'entertainment',
    'native_art',
    '1.0.0',
    'published',
    46000,
    4.9,
    '2026-11-15T00:00:00Z',
    '2026-11-15T00:00:00Z'
);

-- 4. Insert Bot Versions for 15 Flagship Bots
INSERT OR IGNORE INTO bot_versions (
    id, bot_id, version, package_key, package_size, sha256, release_notes, minimum_app_version, minimum_runtime_version, status, published_at
) VALUES
(
    'ver_habit_forge_100',
    'bot_habit_forge',
    '1.0.0',
    'packages/bot_habit_forge_1.0.0.botpkg',
    1195,
    'b654bf751465bc04b1abc438742a86b8dbcad9e535d54b64bea6dca735e65fa0',
    '• Initial release of HabitForge Daily Tracker\n• Telegram habit check-ins, automated evening reviews & streak counters\n• Interactive inline buttons for completion logging',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_penny_wise_100',
    'bot_penny_wise',
    '1.0.0',
    'packages/bot_penny_wise_1.0.0.botpkg',
    1238,
    '4b66663098f2d6bf48980a388e81258a00aa54d52a69297a0f0400b8aa04d6f9',
    '• Initial release of PennyWise Expense Tracker\n• 3-second expense logging via /spend\n• Monthly budget tracking, category analytics & CSV export',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_pomo_focus_100',
    'bot_pomo_focus',
    '1.0.0',
    'packages/bot_pomo_focus_1.0.0.botpkg',
    1213,
    'c7dc91aad3beb7e0dfe392c6192ff02f158c300dd815a7c78e35e0b632876df1',
    '• Initial release of PomoFocus Deep Work Timer\n• 25/5 Pomodoro intervals with phone haptic notifications\n• Task sprint tagging and weekly focus metrics',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_tap_note_100',
    'bot_tap_note',
    '1.0.0',
    'packages/bot_tap_note_1.0.0.botpkg',
    1140,
    'be2a709e4b2c4d519700694a800235cce45d982782d4b924e2bb76df48603ecd',
    '• Initial release of TapNote Scratchpad\n• Fast note capture, interactive todo checklists & micro-journaling\n• Full markdown export and keyword search',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_sky_pulse_100',
    'bot_sky_pulse',
    '1.0.0',
    'packages/bot_sky_pulse_1.0.0.botpkg',
    1225,
    '7e8c686ae8a8fd55ebcaae70c40b701c4a69c395cd7e4007df5764be784d97b5',
    '• Initial release of SkyPulse Weather Radar\n• Current conditions, multi-day forecasts & rain radar\n• Scheduled 7:00 AM morning briefings and severe weather alerts',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_remind_me_100',
    'bot_remind_me',
    '1.0.0',
    'packages/bot_remind_me_1.0.0.botpkg',
    1189,
    '225b527dfdfaac7b78487b1988150ef07a284f2b515846208c762eb50ce3dff8',
    '• Initial release of RemindMe Health & Pill Alarms\n• Natural language reminders and countdown timers\n• Water alarms, recurring medication pings & snooze actions',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_link_drop_100',
    'bot_link_drop',
    '1.0.0',
    'packages/bot_link_drop_1.0.0.botpkg',
    1164,
    '4092c5ff54c9a413a2fa445222b42ff7430aa0e15bf3b03d6f3681f6fa59fd7c',
    '• Initial release of LinkDrop Link & Clipboard Vault\n• Cross-device link saving with topic tags\n• Remote phone clipboard synchronization and bookmark search',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_poly_glot_100',
    'bot_poly_glot',
    '1.0.0',
    'packages/bot_poly_glot_1.0.0.botpkg',
    1197,
    '085e1321d27747a9a2e22859e0722cc859a8c7f5782f960841d8813bd82e6d9f',
    '• Initial release of PolyGlot Instant Translator\n• Fast translation across 50+ languages with auto-detection\n• Daily vocabulary flashcards and interactive learning quizzes',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_coin_radar_100',
    'bot_coin_radar',
    '1.0.0',
    'packages/bot_coin_radar_1.0.0.botpkg',
    1209,
    'a9f900f87edc6d3e7d5ed080513ae742c7609393ef239cff1bb0ee9412d75adc',
    '• Initial release of CoinRadar Crypto & Stock Watcher\n• Real-time price quotes and 7-day ASCII sparkline trend charts\n• Personal portfolio valuation and high/low price trigger alerts',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_fit_track_100',
    'bot_fit_track',
    '1.0.0',
    'packages/bot_fit_track_1.0.0.botpkg',
    1210,
    'ddc0484e75c2f65939c10a3d63bc95de9b5f11eebd014685728fbca6e354a510',
    '• Initial release of FitTrack Hydration & Workouts\n• Workout session logging (run, gym, bike, yoga)\n• Daily hydration target tracking and weekly calorie summaries',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_parcel_track_100',
    'bot_parcel_track',
    '1.0.0',
    'packages/bot_parcel_track_1.0.0.botpkg',
    1166,
    '23aae63eb7025fd678a85bd876d690f3f9768a2b014539d2340a637cda8cdcf2',
    '• Initial release of ParcelTrack Delivery Watcher\n• Multi-courier parcel tracking with custom labels\n• Milestone event timelines and delivery completion alerts',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_git_notifier_100',
    'bot_git_notifier',
    '1.0.0',
    'packages/bot_git_notifier_1.0.0.botpkg',
    1182,
    'de8148aa4edb70b03bf330fd2fc67000f7d8fb5ebbd6d9a41ffe33eaabf4daf1',
    '• Initial release of GitNotifier Repo Watcher\n• GitHub & GitLab repository monitoring for commits, PRs, and issues\n• CI/CD build pipeline alerts and repository growth statistics',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_cam_sentry_100',
    'bot_cam_sentry',
    '1.0.0',
    'packages/bot_cam_sentry_1.0.0.botpkg',
    1225,
    '7ce42a3e6048698231b25bcbd74283414b89577c62aa4185e35aca0571e7ccff',
    '• Initial release of CamSentry Hardware Security\n• Remote photo capture from front/back phone cameras\n• Flash-assisted photography in darkness and device telemetry stats',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_whisper_box_100',
    'bot_whisper_box',
    '1.0.0',
    'packages/bot_whisper_box_1.0.0.botpkg',
    1197,
    '826b3fb90acd06cc543e47a7a4f37b2925fd0e926536517e6c203c4668f7243c',
    '• Initial release of WhisperBox Anonymous Mailbox\n• Shareable public ask links for anonymous feedback\n• Unread question inbox, anonymous replies, and sender moderation',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
),
(
    'ver_trivia_master_100',
    'bot_trivia_master',
    '1.0.0',
    'packages/bot_trivia_master_1.0.0.botpkg',
    1202,
    'e4335519ca4da01088e58a7112b92e6611eb480250df4bb9a92d511864e0caa6',
    '• Initial release of TriviaMaster Daily Brain Quiz\n• Daily 5-question interactive multiple-choice trivia challenge\n• Multi-topic selection, participation streaks, and group leaderboards',
    1,
    '1.0.0',
    'published',
    '2026-11-15T00:00:00Z'
);

-- 5. Insert Bot Credentials for 15 Flagship Bots
INSERT OR IGNORE INTO bot_credentials (id, bot_id, key, display_name, description, required, secret, input_type) VALUES
-- HabitForge
('cred_habit_token', 'bot_habit_forge', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_habit_reminder', 'bot_habit_forge', 'daily_reminder_time', 'Daily Check-in Reminder Time', 'Evening time for daily habit check-in notification (default 20:00)', 0, 0, 'text'),

-- PennyWise
('cred_penny_token', 'bot_penny_wise', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_penny_currency', 'bot_penny_wise', 'currency_symbol', 'Currency Symbol', 'Preferred currency symbol for expense tracking (default $)', 0, 0, 'text'),
('cred_penny_budget', 'bot_penny_wise', 'monthly_budget_target', 'Default Monthly Budget', 'Target monthly spending budget allowance (e.g. 2000)', 0, 0, 'number'),

-- PomoFocus
('cred_pomo_token', 'bot_pomo_focus', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_pomo_minutes', 'bot_pomo_focus', 'default_work_minutes', 'Default Work Sprint Minutes', 'Duration of work sprint in minutes (default 25)', 0, 0, 'number'),

-- TapNote
('cred_note_token', 'bot_tap_note', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),

-- SkyPulse
('cred_sky_token', 'bot_sky_pulse', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_sky_city', 'bot_sky_pulse', 'weather_city', 'Default City / Region', 'Default home city for weather reports and morning briefings (e.g. San Francisco)', 0, 0, 'text'),
('cred_sky_apikey', 'bot_sky_pulse', 'weather_api_key', 'Optional OpenWeatherMap API Key', 'API key from OpenWeatherMap for live atmospheric feeds', 0, 1, 'password'),

-- RemindMe
('cred_remind_token', 'bot_remind_me', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),

-- LinkDrop
('cred_link_token', 'bot_link_drop', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),

-- PolyGlot
('cred_poly_token', 'bot_poly_glot', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_poly_target', 'bot_poly_glot', 'default_target_lang', 'Default Target Language Code', 'ISO 639-1 language code for quick translation (default es)', 0, 0, 'text'),

-- CoinRadar
('cred_coin_token', 'bot_coin_radar', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_coin_currency', 'bot_coin_radar', 'crypto_currency', 'Quote Currency', 'Fiat or stablecoin quote currency symbol (default USD)', 0, 0, 'text'),

-- FitTrack
('cred_fit_token', 'bot_fit_track', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_fit_water', 'bot_fit_track', 'daily_water_target_ml', 'Daily Water Target (ml)', 'Daily hydration goal in milliliters (default 2500)', 0, 0, 'number'),

-- ParcelTrack
('cred_parcel_token', 'bot_parcel_track', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),

-- GitNotifier
('cred_git_token', 'bot_git_notifier', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_git_pat', 'bot_git_notifier', 'github_token', 'GitHub Personal Access Token', 'Personal access token for watching private repositories and avoiding rate limits', 0, 1, 'password'),

-- CamSentry
('cred_cam_token', 'bot_cam_sentry', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_cam_uid', 'bot_cam_sentry', 'authorized_user_id', 'Authorized Commander User ID', 'Numeric Telegram user ID permitted to trigger camera snapshots', 0, 0, 'text'),

-- WhisperBox
('cred_whisper_token', 'bot_whisper_box', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_whisper_owner', 'bot_whisper_box', 'inbox_owner_id', 'Owner User ID', 'Numeric Telegram user ID that receives all anonymous inbox messages', 0, 0, 'text'),

-- TriviaMaster
('cred_trivia_token', 'bot_trivia_master', 'bot_token', 'Telegram Bot Token', 'Obtain via @BotFather on Telegram (e.g. 123456:ABC-DEF...)', 1, 1, 'password'),
('cred_trivia_diff', 'bot_trivia_master', 'difficulty_level', 'Difficulty Level', 'Quiz difficulty level: easy, medium, hard, or mixed (default medium)', 0, 0, 'text');
