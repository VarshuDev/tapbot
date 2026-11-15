package com.tapbot.core.network

import com.tapbot.core.model.BotCredentialSpec
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
import com.tapbot.core.model.BotVersion

/**
 * Interface to fetch catalog metadata published by the developer.
 * Hosted on Cloudflare Workers / D1 / R2.
 */
interface CatalogApi {
    suspend fun getBots(): Result<List<BotMetadata>>
    suspend fun getBotDetails(botId: String): Result<BotMetadata>
    suspend fun getBotVersions(botId: String): Result<List<BotVersion>>
    suspend fun getLatestVersion(botId: String): Result<BotVersion?>
}

/**
 * Built-in mock implementation for offline previews, testing, and initial boot.
 */
class MockCatalogApi : CatalogApi {

    private val sampleBots = listOf(
        // 1. TapBot Commander
        BotMetadata(
            id = "bot_commander",
            name = "TapBot Commander",
            summary = "Flagship on-device remote controller, device watchdog, and fleet manager for your Android phone.",
            description = "The premier flagship on-device Telegram bot for TapBot. Runs natively on Android ART to provide real-time hardware telemetry (battery, RAM, disk, network, uptime), bot fleet management (/bots, /startbot, /stopbot, /restartbot, /logs), remote phone controls (ring phone, flashlight, notifications, clipboard, vibrate), APK delivery directly in chat with SHA-256 integrity verification, and autonomous watchdog alerts for low battery and bot crashes.",
            author = "TapBot Core Team",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=200",
            category = "Automation",
            tags = listOf("flagship", "commander", "telemetry", "fleet", "watchdog", "remote"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "owner_id",
                    label = "Bot Owner User ID",
                    description = "Numeric Telegram user ID from @userinfobot to authorize sensitive device commands (e.g. 7342044274)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "7342044274"
                ),
                BotCredentialSpec(
                    key = "low_battery_threshold",
                    label = "Low Battery Threshold (%)",
                    description = "Battery percentage that triggers an urgent Telegram alert (default 15)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "15"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/tapbot-commander_1.0.0.botpkg",
                sha256Checksum = "51dd47dec8e9bbc0c88b094887c1a05da4c6835c2894f350986e25684edeb7f3",
                runtimeType = "native_art",
                packageSizeBytes = 1703L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE", "POST_NOTIFICATIONS", "VIBRATE", "FLASHLIGHT", "ACCESS_WIFI_STATE", "ACCESS_COARSE_LOCATION")
        ),

        // 2. TapStream Music Bot
        BotMetadata(
            id = "bot_music_streamer",
            name = "TapStream Music Bot",
            summary = "On-device music streaming, YouTube/yt-dlp resolution, 24/7 internet radio stations, and Android speaker playback.",
            description = "TapStream is an on-device music streaming and audio distribution bot for Telegram running natively on Android ART. Features dual audio output: streams music out loud through your phone speaker or Bluetooth audio system, and delivers playable audio tracks directly into Telegram chats via sendAudio. Integrates yt-dlp API resolution, direct audio stream playback (.mp3, .m4a, .aac, .ogg, .opus), 6 curated 24/7 internet radio stations, interactive queue management, dynamic ASCII now-playing HUD, and hardware volume control.",
            author = "TapBot Audio Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200",
            category = "Media",
            tags = listOf("music", "audio", "streaming", "radio", "ytdlp", "speaker"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "stream_resolver_url",
                    label = "Custom yt-dlp Resolver API URL",
                    description = "Optional custom yt-dlp, Cobalt, or Piped resolver endpoint",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "https://ytdlp.example.com/api/resolve"
                ),
                BotCredentialSpec(
                    key = "default_volume",
                    label = "Default Speaker Volume (%)",
                    description = "Initial Android phone speaker volume from 0 to 100 (default 80)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "80"
                ),
                BotCredentialSpec(
                    key = "default_playback_mode",
                    label = "Default Playback Mode",
                    description = "both (speaker + chat), speaker (phone only), or chat (telegram only)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "both"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/tapstream-music-1.0.0.botpkg",
                sha256Checksum = "178434b2d9ff07199455a3ae04373be2d13bec341e398baa99529c4635550b0a",
                runtimeType = "native_art",
                packageSizeBytes = 1827L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE", "MODIFY_AUDIO_SETTINGS", "WAKE_LOCK", "FOREGROUND_SERVICE_MEDIA_PLAYBACK")
        ),

        // 3. Exon Robot Sentinel
        BotMetadata(
            id = "bot_1790084493920_n4l8lj",
            name = "Exon Robot",
            summary = "Advanced Telegram group management bot with ban, mute, pin, and moderation capabilities.",
            description = "High-performance Telegram group management bot inspired by ExonRobot. Runs directly on your Android device via ART runtime with MTProto, timed moderation, persisted locks & antiflood, welcome greetings, and inline controls.",
            author = "VarshuDev",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200",
            category = "Automation",
            tags = listOf("management", "telegram", "moderation", "admin"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "app_api_id",
                    label = "Telegram API ID (api_id)",
                    description = "Your Telegram App API ID obtained from my.telegram.org",
                    isSecret = false,
                    isRequired = true,
                    placeholder = "12345678"
                ),
                BotCredentialSpec(
                    key = "app_api_hash",
                    label = "Telegram API Hash (api_hash)",
                    description = "Your Telegram App API Hash obtained from my.telegram.org",
                    isSecret = true,
                    isRequired = true,
                    placeholder = "0123456789abcdef0123456789abcdef"
                ),
                BotCredentialSpec(
                    key = "owner_id",
                    label = "Bot Owner User ID",
                    description = "Numeric Telegram user ID of the bot master / owner",
                    isSecret = false,
                    isRequired = true,
                    placeholder = "7342044274"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_exon_1.0.0.botpkg",
                sha256Checksum = "e269c5351b048201ad1b7e417e954c47a2a745e0d8522b68ff2de03a581d65d0",
                runtimeType = "native_art",
                packageSizeBytes = 1213L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 4. Gemini Smart Assistant
        BotMetadata(
            id = "bot_ai_assistant",
            name = "Gemini Smart Assistant",
            summary = "Conversational AI assistant powered by Google Gemini running directly through your personal Telegram account.",
            description = "Turns your personal Telegram bot into a generative AI assistant. Supports multi-turn chats, code analysis, and summarization right in Telegram. Your Gemini API key and Telegram token remain encrypted in your device Keystore.",
            author = "TapBot Labs",
            version = "2.1.0",
            iconUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=200",
            category = "Productivity",
            tags = listOf("ai", "gemini", "assistant", "productivity"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "gemini_api_key",
                    label = "Gemini API Key",
                    description = "Obtain a free Google AI Studio API key at https://aistudio.google.com",
                    isSecret = true,
                    isRequired = true,
                    placeholder = "AIzaSy...",
                    helpUrl = "https://aistudio.google.com"
                ),
                BotCredentialSpec(
                    key = "gemini_model",
                    label = "Gemini Model",
                    description = "Optional model ID (default: gemini-2.5-flash)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "gemini-2.5-flash"
                ),
                BotCredentialSpec(
                    key = "system_prompt",
                    label = "System Instruction",
                    description = "Optional custom persona or instructions sent with every prompt",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "You are a concise, helpful assistant."
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_ai_assistant_2.1.0.botpkg",
                sha256Checksum = "44c098853cd5d46484141cefa70e3bbb763915e30c71848bd67316cb8ff9c185",
                runtimeType = "built-in",
                entryClass = "com.tapbot.core.runner.runtime.AIBotRuntime",
                packageSizeBytes = 1212L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 5. HabitForge Daily Tracker
        BotMetadata(
            id = "bot_habit_forge",
            name = "HabitForge Daily Tracker",
            summary = "Daily habit tracking with streak preservation, scheduled check-in reminders, and progress analytics.",
            description = "Build lasting habits with daily Telegram check-ins, automated evening reviews, streak tracking, and interactive inline buttons. Runs entirely on your Android device with zero cloud storage.",
            author = "Forge Life Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=200",
            category = "Lifestyle",
            tags = listOf("habit", "lifestyle", "streak", "goals", "daily"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "daily_reminder_time",
                    label = "Daily Check-in Reminder Time",
                    description = "24-hour format HH:MM (e.g. 20:00) for daily reminder check-ins",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "20:00"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_habit_forge_1.0.0.botpkg",
                sha256Checksum = "b654bf751465bc04b1abc438742a86b8dbcad9e535d54b64bea6dca735e65fa0",
                runtimeType = "native_art",
                packageSizeBytes = 1195L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 6. PennyWise Expense Tracker
        BotMetadata(
            id = "bot_penny_wise",
            name = "PennyWise Expense Tracker",
            summary = "3-second expense logging, daily spend limits, monthly budget analytics, and CSV transaction export.",
            description = "Fast, friction-free personal finance tracker. Log expenses in 3 seconds directly via Telegram chat commands, track spending categories, visualize monthly budget progress, and export CSV ledgers.",
            author = "PennyWise Financial",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=200",
            category = "Finance",
            tags = listOf("finance", "budget", "expenses", "money", "ledger"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "currency_symbol",
                    label = "Currency Symbol",
                    description = "Preferred currency symbol for expense tracking (default $)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "$"
                ),
                BotCredentialSpec(
                    key = "monthly_budget_target",
                    label = "Default Monthly Budget",
                    description = "Target monthly spending budget allowance (e.g. 2000)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "2000"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_penny_wise_1.0.0.botpkg",
                sha256Checksum = "4b66663098f2d6bf48980a388e81258a00aa54d52a69297a0f0400b8aa04d6f9",
                runtimeType = "native_art",
                packageSizeBytes = 1238L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 7. PomoFocus Deep Work Timer
        BotMetadata(
            id = "bot_pomo_focus",
            name = "PomoFocus Deep Work Timer",
            summary = "25/5 Pomodoro intervals, task sprint logging, phone haptic vibration alerts, and focus analytics.",
            description = "On-device deep work companion with classic 25/5 interval cycles, short/long break sequences, haptic feedback triggers, sprint statistics, and task tagging.",
            author = "FocusCraft Studio",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=200",
            category = "Productivity",
            tags = listOf("pomodoro", "timer", "focus", "productivity", "sprint"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "default_work_minutes",
                    label = "Default Work Sprint Minutes",
                    description = "Duration of work sprint in minutes (default 25)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "25"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_pomo_focus_1.0.0.botpkg",
                sha256Checksum = "c7dc91aad3beb7e0dfe392c6192ff02f158c300dd815a7c78e35e0b632876df1",
                runtimeType = "native_art",
                packageSizeBytes = 1213L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE", "VIBRATE")
        ),

        // 8. TapNote Scratchpad
        BotMetadata(
            id = "bot_tap_note",
            name = "TapNote Scratchpad",
            summary = "Rapid thought capture, todo checklists, daily micro-journaling, search, and Markdown export.",
            description = "Lightweight scratchpad for capturing fleeting ideas, maintaining active todo checklists, logging micro-journal reflections, and exporting clean markdown notes directly in chat.",
            author = "TapNote Systems",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1517842645767-c639042777db?w=200",
            category = "Productivity",
            tags = listOf("notes", "todo", "scratchpad", "journal", "markdown"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_tap_note_1.0.0.botpkg",
                sha256Checksum = "be2a709e4b2c4d519700694a800235cce45d982782d4b924e2bb76df48603ecd",
                runtimeType = "native_art",
                packageSizeBytes = 1140L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 9. SkyPulse Weather Radar
        BotMetadata(
            id = "bot_sky_pulse",
            name = "SkyPulse Weather Radar",
            summary = "Live weather radar, 7 AM morning briefing, multi-day forecasts, and severe storm alerts.",
            description = "Real-time atmospheric monitoring bot providing instant temperature, humidity, wind conditions, multi-day forecasts, severe weather warnings, and scheduled 7:00 AM morning briefings.",
            author = "SkyPulse Weather",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1534088568595-a066f410bcda?w=200",
            category = "Utilities",
            tags = listOf("weather", "forecast", "radar", "alerts", "temperature"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "weather_city",
                    label = "Default City / Region",
                    description = "Default home city for forecasts and daily briefings",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "San Francisco"
                ),
                BotCredentialSpec(
                    key = "weather_api_key",
                    label = "Optional OpenWeatherMap API Key",
                    description = "API key from OpenWeatherMap for live atmospheric feeds",
                    isSecret = true,
                    isRequired = false,
                    placeholder = "Optional API key"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_sky_pulse_1.0.0.botpkg",
                sha256Checksum = "7e8c686ae8a8fd55ebcaae70c40b701c4a69c395cd7e4007df5764be784d97b5",
                runtimeType = "native_art",
                packageSizeBytes = 1225L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 10. RemindMe Health & Pill Alarms
        BotMetadata(
            id = "bot_remind_me",
            name = "RemindMe Health & Pill Alarms",
            summary = "Natural language reminders, water alarms, recurring medication pill schedules, and snooze controls.",
            description = "Reliable on-device alarm bot for scheduling natural language reminders, recurring medication doses, periodic hydration pings, and interactive snooze buttons.",
            author = "PulseHealth Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=200",
            category = "Lifestyle",
            tags = listOf("reminders", "health", "alarms", "pills", "water"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_remind_me_1.0.0.botpkg",
                sha256Checksum = "225b527dfdfaac7b78487b1988150ef07a284f2b515846208c762eb50ce3dff8",
                runtimeType = "native_art",
                packageSizeBytes = 1189L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE", "POST_NOTIFICATIONS", "VIBRATE")
        ),

        // 11. LinkDrop Link & Clipboard Vault
        BotMetadata(
            id = "bot_link_drop",
            name = "LinkDrop Link & Clipboard Vault",
            summary = "Save articles and URLs from PC to phone, tag bookmarks, and copy directly to Android clipboard.",
            description = "Cross-device bookmarking vault. Drop links from your desktop or phone into Telegram, organize with topic tags, search full URLs, and copy saved snippets directly to your Android clipboard.",
            author = "DropLink Systems",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=200",
            category = "Utilities",
            tags = listOf("links", "bookmarks", "clipboard", "vault", "sync"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_link_drop_1.0.0.botpkg",
                sha256Checksum = "4092c5ff54c9a413a2fa445222b42ff7430aa0e15bf3b03d6f3681f6fa59fd7c",
                runtimeType = "native_art",
                packageSizeBytes = 1164L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 12. PolyGlot Instant Translator
        BotMetadata(
            id = "bot_poly_glot",
            name = "PolyGlot Instant Translator",
            summary = "Instant multi-language translation in chat, language auto-detection, and daily vocabulary learning.",
            description = "Pocket translator supporting over 50 languages. Translate phrases instantly with ISO language codes, detect source languages with confidence scoring, and practice daily vocabulary flashcards.",
            author = "Linguist AI",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=200",
            category = "Utilities",
            tags = listOf("translator", "languages", "dictionary", "vocabulary", "learning"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "default_target_lang",
                    label = "Default Target Language Code",
                    description = "Two-letter ISO 639-1 code (e.g. es, fr, de, ja)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "es"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_poly_glot_1.0.0.botpkg",
                sha256Checksum = "085e1321d27747a9a2e22859e0722cc859a8c7f5782f960841d8813bd82e6d9f",
                runtimeType = "native_art",
                packageSizeBytes = 1197L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 13. CoinRadar Crypto & Stock Watcher
        BotMetadata(
            id = "bot_coin_radar",
            name = "CoinRadar Crypto & Stock Watcher",
            summary = "Live crypto and stock quotes, 7-day ASCII sparklines, local portfolio P&L, and price trigger alerts.",
            description = "Financial market watcher delivering real-time quotes, 7-day ASCII sparkline trend charts, personal portfolio valuation, top crypto market rankings, and customizable price trigger notifications.",
            author = "CoinRadar Analytics",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1621416894569-0f39ed31d247?w=200",
            category = "Finance",
            tags = listOf("crypto", "stocks", "bitcoin", "prices", "portfolio", "alerts"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "crypto_currency",
                    label = "Quote Currency",
                    description = "Fiat or stablecoin quote currency symbol (default USD)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "USD"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_coin_radar_1.0.0.botpkg",
                sha256Checksum = "a9f900f87edc6d3e7d5ed080513ae742c7609393ef239cff1bb0ee9412d75adc",
                runtimeType = "native_art",
                packageSizeBytes = 1209L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 14. FitTrack Hydration & Workouts
        BotMetadata(
            id = "bot_fit_track",
            name = "FitTrack Hydration & Workouts",
            summary = "Daily water intake tracker, workout activity logger, active calorie burn tally, and body weight logs.",
            description = "Personal fitness companion for logging workouts across multiple disciplines (running, gym, cycling, yoga), tracking daily water intake, monitoring active calorie burn, and viewing weekly stats.",
            author = "FitPulse Athletics",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=200",
            category = "Lifestyle",
            tags = listOf("fitness", "workout", "water", "calories", "health"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "daily_water_target_ml",
                    label = "Daily Water Target (ml)",
                    description = "Daily hydration goal in milliliters (default 2500)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "2500"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_fit_track_1.0.0.botpkg",
                sha256Checksum = "ddc0484e75c2f65939c10a3d63bc95de9b5f11eebd014685728fbca6e354a510",
                runtimeType = "native_art",
                packageSizeBytes = 1210L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 15. ParcelTrack Delivery Watcher
        BotMetadata(
            id = "bot_parcel_track",
            name = "ParcelTrack Delivery Watcher",
            summary = "Multi-carrier shipment tracking (FedEx, UPS, DHL, USPS, Amazon) with milestone event timeline alerts.",
            description = "Shipment tracking hub supporting global couriers. Add parcel tracking numbers with custom labels, monitor milestone progress timelines, and receive alerts upon delivery completion.",
            author = "ParcelTrack Global",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=200",
            category = "Utilities",
            tags = listOf("shipping", "delivery", "tracking", "packages", "courier"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_parcel_track_1.0.0.botpkg",
                sha256Checksum = "23aae63eb7025fd678a85bd876d690f3f9768a2b014539d2340a637cda8cdcf2",
                runtimeType = "native_art",
                packageSizeBytes = 1166L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 16. NewsPulse RSS Broadcaster
        BotMetadata(
            id = "bot_rss_notifier",
            name = "RSS Channel Broadcaster",
            summary = "Periodically checks your favorite RSS/Atom feeds and broadcasts new articles to your Telegram channel.",
            description = "Runs on your Android device 24/7 with full RSS 2.0 and Atom 1.0 feed parsing, persisted seen-GUID deduplication across restarts, and interactive /feed, /check, and /clearcache commands.",
            author = "OpenFeed Community",
            version = "1.2.0",
            iconUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200",
            category = "Media",
            tags = listOf("rss", "channel", "news", "automation"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "target_chat_id",
                    label = "Target Chat or Channel ID",
                    description = "Channel or Group ID (e.g. -1001234567890) where messages will be posted.",
                    isSecret = false,
                    isRequired = true,
                    placeholder = "-1001234567890"
                ),
                BotCredentialSpec(
                    key = "feed_url",
                    label = "RSS / Atom Feed URL",
                    description = "HTTP/HTTPS URL of the RSS feed to monitor.",
                    isSecret = false,
                    isRequired = true,
                    placeholder = "https://news.ycombinator.com/rss"
                ),
                BotCredentialSpec(
                    key = "poll_interval_minutes",
                    label = "Poll Interval (minutes)",
                    description = "How often to scan subscribed feeds for new posts (default: 15)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "15"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_rss_1.2.0.botpkg",
                sha256Checksum = "91f367ada04dae04ff3536a8a334d0b18aa411fb51faded21d17ce775663015b",
                runtimeType = "built-in",
                entryClass = "com.tapbot.core.runner.runtime.RssNotifierBotRuntime",
                packageSizeBytes = 1248L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 17. GitNotifier Repo Watcher
        BotMetadata(
            id = "bot_git_notifier",
            name = "GitNotifier Repo Watcher",
            summary = "Monitors GitHub and GitLab repositories for commits, pull requests, issues, and CI/CD workflow status.",
            description = "Developer repository sentinel. Monitor public or private GitHub and GitLab repositories for incoming commits, pull request review requests, issue discussions, and CI/CD workflow pass/fail statuses.",
            author = "GitNotifier Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1618401471353-b98afee0b2eb?w=200",
            category = "Developer Tools",
            tags = listOf("git", "github", "gitlab", "commits", "devops", "ci"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "github_token",
                    label = "GitHub Personal Access Token",
                    description = "Personal access token for watching private repositories and avoiding rate limits",
                    isSecret = true,
                    isRequired = false,
                    placeholder = "ghp_..."
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_git_notifier_1.0.0.botpkg",
                sha256Checksum = "de8148aa4edb70b03bf330fd2fc67000f7d8fb5ebbd6d9a41ffe33eaabf4daf1",
                runtimeType = "native_art",
                packageSizeBytes = 1182L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 18. CamSentry Hardware Security
        BotMetadata(
            id = "bot_cam_sentry",
            name = "CamSentry Hardware Security",
            summary = "Remote phone camera photo capture (front/back), LED flash snap, security watchdog, and hardware status.",
            description = "Hardware surveillance tool leveraging your Android phone camera. Trigger remote photo snapshots from front or back lenses, capture flash photos in darkness, and receive hardware telemetry reports.",
            author = "SentryTech Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1557597774-9d273605dfa9?w=200",
            category = "Automation",
            tags = listOf("camera", "security", "surveillance", "hardware", "photos"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "authorized_user_id",
                    label = "Authorized Commander User ID",
                    description = "Numeric Telegram user ID permitted to trigger camera snapshots",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "7342044274"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_cam_sentry_1.0.0.botpkg",
                sha256Checksum = "7ce42a3e6048698231b25bcbd74283414b89577c62aa4185e35aca0571e7ccff",
                runtimeType = "native_art",
                packageSizeBytes = 1225L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE", "CAMERA", "FLASHLIGHT")
        ),

        // 19. WhisperBox Anonymous Mailbox
        BotMetadata(
            id = "bot_whisper_box",
            name = "WhisperBox Anonymous Mailbox",
            summary = "Shareable anonymous question and feedback inbox for group members, followers, and creator channels.",
            description = "Private anonymous feedback inbox. Generate unique question links, receive unread anonymous inquiries safely, reply anonymously back to senders, and manage moderation blocks.",
            author = "WhisperBox Community",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1577563908411-5077b6dc7624?w=200",
            category = "Social",
            tags = listOf("anonymous", "mailbox", "feedback", "questions", "social"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "inbox_owner_id",
                    label = "Owner User ID",
                    description = "Numeric Telegram user ID that receives all anonymous inbox messages",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "7342044274"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_whisper_box_1.0.0.botpkg",
                sha256Checksum = "826b3fb90acd06cc543e47a7a4f37b2925fd0e926536517e6c203c4668f7243c",
                runtimeType = "native_art",
                packageSizeBytes = 1197L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // 20. TriviaMaster Daily Brain Quiz
        BotMetadata(
            id = "bot_trivia_master",
            name = "TriviaMaster Daily Brain Quiz",
            summary = "Daily 5-question trivia challenge, topic selection, interactive inline quiz polls, and streak leaderboards.",
            description = "Interactive daily knowledge game. Engage in 5-question multi-topic quizzes (Science, History, Technology, Geography), tackle the Daily Challenge question, maintain streaks, and climb score leaderboards.",
            author = "TriviaMaster Games",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?w=200",
            category = "Entertainment",
            tags = listOf("trivia", "quiz", "games", "learning", "brain"),
            isFeatured = true,
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "difficulty_level",
                    label = "Difficulty Level",
                    description = "easy, medium, hard, or mixed",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "medium"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "packages/bot_trivia_master_1.0.0.botpkg",
                sha256Checksum = "e4335519ca4da01088e58a7112b92e6611eb480250df4bb9a92d511864e0caa6",
                runtimeType = "native_art",
                packageSizeBytes = 1202L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),

        // Legacy / starter bots preserved for backward compatibility
        BotMetadata(
            id = "bot_echo",
            name = "Echo Assistant Bot",
            summary = "A lightweight Telegram bot that responds back with structured diagnostics and echoes user messages.",
            description = "Ideal for testing your Telegram Bot Token and verifying on-device foreground execution. The bot listens for incoming messages via Telegram long-polling and replies back with message metadata, latency, and uptime.",
            author = "TapBot Team",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200",
            category = "Utilities",
            tags = listOf("echo", "test", "starter"),
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "https://assets.tapbot.internal/packages/bot_echo_1.0.0.botpkg",
                sha256Checksum = "be89e34e178e8be6643a5e7dc0416d7309f43cb140f627080b33f1002f23cbff",
                runtimeType = "built-in",
                entryClass = "com.tapbot.core.runner.engine.EchoBotEngine",
                packageSizeBytes = 1113L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),
        BotMetadata(
            id = "bot_music",
            name = "Music Bot",
            summary = "Streams audio, queues playlists, and provides music playback controls.",
            description = "Runs locally on Android to manage Telegram audio playback queues, interactive commands, and volume control.",
            author = "TapBot Audio Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200",
            category = "Media",
            tags = listOf("music", "audio", "player"),
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "spotify_client_id",
                    label = "Spotify Client ID",
                    description = "Optional Spotify integration client identifier",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "Optional client id"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "https://assets.tapbot.internal/packages/bot_music_1.0.0.botpkg",
                sha256Checksum = "8c12f7ae7b3134dc5a27ee7c8c0b5396f6101fe1b97b26d61da498d926f7b391",
                runtimeType = "native_art",
                packageSizeBytes = 1139L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),
        BotMetadata(
            id = "bot_ai",
            name = "AI Bot",
            summary = "Generative AI conversational assistant powered on-device.",
            description = "Provides multi-turn AI reasoning, text summarization, and code explanation directly through your personal Telegram bot.",
            author = "TapBot AI Labs",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=200",
            category = "Productivity",
            tags = listOf("ai", "assistant", "productivity"),
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                BotCredentialSpec(
                    key = "gemini_api_key",
                    label = "Gemini API Key",
                    description = "Optional Google AI Studio API key (https://aistudio.google.com) for live Gemini responses",
                    isSecret = true,
                    isRequired = false,
                    placeholder = "AIzaSy...",
                    helpUrl = "https://aistudio.google.com"
                ),
                BotCredentialSpec(
                    key = "gemini_model",
                    label = "Gemini Model",
                    description = "Optional Gemini model name (default: gemini-2.5-flash)",
                    isSecret = false,
                    isRequired = false,
                    placeholder = "gemini-2.5-flash"
                )
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "https://assets.tapbot.internal/packages/bot_ai_1.0.0.botpkg",
                sha256Checksum = "f8f130c0af3050937663f80df9a09766aa743dc7a4a99444d25abe4246913b0e",
                runtimeType = "native_art",
                packageSizeBytes = 1139L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),
        BotMetadata(
            id = "bot_utility",
            name = "Utility Bot",
            summary = "System diagnostics, uptime monitoring, latency checks, and test crash simulation.",
            description = "Monitors Android ART execution metrics, memory usage, and allows resilience and crash testing via /crash.",
            author = "TapBot Core",
            version = "1.0.0",
            iconUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=200",
            category = "Utilities",
            tags = listOf("utility", "diagnostics", "tools"),
            requiredCredentials = listOf(
                BotCredentialSpec.TELEGRAM_BOT_TOKEN
            ),
            packageInfo = BotPackageInfo(
                packageUrl = "https://assets.tapbot.internal/packages/bot_utility_1.0.0.botpkg",
                sha256Checksum = "18207df14e1e7d811cf672f930e5a7286dedd331653b47224e8da4cca6f1663b",
                runtimeType = "native_art",
                packageSizeBytes = 1104L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        )
    )

    private val sampleVersions: Map<String, List<BotVersion>> = buildMap {
        put("bot_commander", listOf(
            BotVersion(
                id = "ver_commander_100",
                botId = "bot_commander",
                version = "1.0.0",
                packageUrl = "packages/tapbot-commander_1.0.0.botpkg",
                sha256 = "51dd47dec8e9bbc0c88b094887c1a05da4c6835c2894f350986e25684edeb7f3",
                packageSize = 1703L,
                releaseNotes = "• Flagship on-device remote controller & fleet manager\n• Real-time hardware telemetry (/status, /battery, /ram, /disk)\n• Remote phone controls (/ring, /torch, /notify, /clipboard, /vibrate, /tts, /wifi, /location)\n• Direct APK document delivery via Telegram (/apk, /download)\n• Autonomous background watchdog, scheduled alerts & inline HUD",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-10-22T00:00:00Z"
            )
        ))
        put("bot_commander_flagship", listOf(
            BotVersion(
                id = "ver_commander_100",
                botId = "bot_commander_flagship",
                version = "1.0.0",
                packageUrl = "packages/tapbot-commander_1.0.0.botpkg",
                sha256 = "51dd47dec8e9bbc0c88b094887c1a05da4c6835c2894f350986e25684edeb7f3",
                packageSize = 1703L,
                releaseNotes = "• Flagship on-device remote controller & fleet manager\n• Real-time hardware telemetry (/status, /battery, /ram, /disk)\n• Remote phone controls (/ring, /torch, /notify, /clipboard, /vibrate, /tts, /wifi, /location)\n• Direct APK document delivery via Telegram (/apk, /download)\n• Autonomous background watchdog, scheduled alerts & inline HUD",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-10-22T00:00:00Z"
            )
        ))
        put("bot_music_streamer", listOf(
            BotVersion(
                id = "ver_streamer_100",
                botId = "bot_music_streamer",
                version = "1.0.0",
                packageUrl = "packages/tapstream-music-1.0.0.botpkg",
                sha256 = "178434b2d9ff07199455a3ae04373be2d13bec341e398baa99529c4635550b0a",
                packageSize = 1827L,
                releaseNotes = "• Initial release of TapStream Music Bot\n• Dual phone speaker & Telegram in-chat playback with AudioFocus\n• YouTube/yt-dlp stream resolution & 24/7 internet radio stations\n• Full queue navigation, /shuffle, /loop, /seek, and inline player controls",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-04T00:00:00Z"
            )
        ))
        val exonVersions = listOf(
            BotVersion(
                id = "ver_exon_100",
                botId = "bot_1790084493920_n4l8lj",
                version = "1.0.0",
                packageUrl = "packages/bot_exon_1.0.0.botpkg",
                sha256 = "e269c5351b048201ad1b7e417e954c47a2a745e0d8522b68ff2de03a581d65d0",
                packageSize = 1213L,
                releaseNotes = "• Exon Robot group management for TapBot ART runtime\n• Timed /tmute, /tban, /promote, /demote, persisted /locks, /setwelcome, /antiflood, and inline menus",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-10-22T00:00:00Z"
            )
        )
        put("bot_1790084493920_n4l8lj", exonVersions)
        put("bot_exon", exonVersions)
        put("bot_ai_assistant", listOf(
            BotVersion(
                id = "ver_ai_assistant_210",
                botId = "bot_ai_assistant",
                version = "2.1.0",
                packageUrl = "packages/bot_ai_assistant_2.1.0.botpkg",
                sha256 = "44c098853cd5d46484141cefa70e3bbb763915e30c71848bd67316cb8ff9c185",
                packageSize = 1212L,
                releaseNotes = "• Real Google Gemini API integration with per-chat conversation history\n• /ask, /summarize, /code, /model, /system, /clear and 4096-char auto chunking",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-10-01T00:00:00Z"
            )
        ))
        put("bot_habit_forge", listOf(
            BotVersion(
                id = "ver_habit_forge_100",
                botId = "bot_habit_forge",
                version = "1.0.0",
                packageUrl = "packages/bot_habit_forge_1.0.0.botpkg",
                sha256 = "b654bf751465bc04b1abc438742a86b8dbcad9e535d54b64bea6dca735e65fa0",
                packageSize = 1195L,
                releaseNotes = "• Initial release of HabitForge Daily Tracker\n• Telegram habit check-ins, automated evening reviews & streak counters\n• Interactive inline buttons for completion logging",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_penny_wise", listOf(
            BotVersion(
                id = "ver_penny_wise_100",
                botId = "bot_penny_wise",
                version = "1.0.0",
                packageUrl = "packages/bot_penny_wise_1.0.0.botpkg",
                sha256 = "4b66663098f2d6bf48980a388e81258a00aa54d52a69297a0f0400b8aa04d6f9",
                packageSize = 1238L,
                releaseNotes = "• Initial release of PennyWise Expense Tracker\n• 3-second expense logging via /spend\n• Monthly budget tracking, category analytics & CSV export",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_pomo_focus", listOf(
            BotVersion(
                id = "ver_pomo_focus_100",
                botId = "bot_pomo_focus",
                version = "1.0.0",
                packageUrl = "packages/bot_pomo_focus_1.0.0.botpkg",
                sha256 = "c7dc91aad3beb7e0dfe392c6192ff02f158c300dd815a7c78e35e0b632876df1",
                packageSize = 1213L,
                releaseNotes = "• Initial release of PomoFocus Deep Work Timer\n• 25/5 Pomodoro intervals with phone haptic notifications\n• Task sprint tagging and weekly focus metrics",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_tap_note", listOf(
            BotVersion(
                id = "ver_tap_note_100",
                botId = "bot_tap_note",
                version = "1.0.0",
                packageUrl = "packages/bot_tap_note_1.0.0.botpkg",
                sha256 = "be2a709e4b2c4d519700694a800235cce45d982782d4b924e2bb76df48603ecd",
                packageSize = 1140L,
                releaseNotes = "• Initial release of TapNote Scratchpad\n• Fast note capture, interactive todo checklists & micro-journaling\n• Full markdown export and keyword search",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_sky_pulse", listOf(
            BotVersion(
                id = "ver_sky_pulse_100",
                botId = "bot_sky_pulse",
                version = "1.0.0",
                packageUrl = "packages/bot_sky_pulse_1.0.0.botpkg",
                sha256 = "7e8c686ae8a8fd55ebcaae70c40b701c4a69c395cd7e4007df5764be784d97b5",
                packageSize = 1225L,
                releaseNotes = "• Initial release of SkyPulse Weather Radar\n• Current conditions, multi-day forecasts & rain radar\n• Scheduled 7:00 AM morning briefings and severe weather alerts",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_remind_me", listOf(
            BotVersion(
                id = "ver_remind_me_100",
                botId = "bot_remind_me",
                version = "1.0.0",
                packageUrl = "packages/bot_remind_me_1.0.0.botpkg",
                sha256 = "225b527dfdfaac7b78487b1988150ef07a284f2b515846208c762eb50ce3dff8",
                packageSize = 1189L,
                releaseNotes = "• Initial release of RemindMe Health & Pill Alarms\n• Natural language reminders and countdown timers\n• Water alarms, recurring medication pings & snooze actions",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_link_drop", listOf(
            BotVersion(
                id = "ver_link_drop_100",
                botId = "bot_link_drop",
                version = "1.0.0",
                packageUrl = "packages/bot_link_drop_1.0.0.botpkg",
                sha256 = "4092c5ff54c9a413a2fa445222b42ff7430aa0e15bf3b03d6f3681f6fa59fd7c",
                packageSize = 1164L,
                releaseNotes = "• Initial release of LinkDrop Link & Clipboard Vault\n• Cross-device link saving with topic tags\n• Remote phone clipboard synchronization and bookmark search",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_poly_glot", listOf(
            BotVersion(
                id = "ver_poly_glot_100",
                botId = "bot_poly_glot",
                version = "1.0.0",
                packageUrl = "packages/bot_poly_glot_1.0.0.botpkg",
                sha256 = "085e1321d27747a9a2e22859e0722cc859a8c7f5782f960841d8813bd82e6d9f",
                packageSize = 1197L,
                releaseNotes = "• Initial release of PolyGlot Instant Translator\n• Fast translation across 50+ languages with auto-detection\n• Daily vocabulary flashcards and interactive learning quizzes",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_coin_radar", listOf(
            BotVersion(
                id = "ver_coin_radar_100",
                botId = "bot_coin_radar",
                version = "1.0.0",
                packageUrl = "packages/bot_coin_radar_1.0.0.botpkg",
                sha256 = "a9f900f87edc6d3e7d5ed080513ae742c7609393ef239cff1bb0ee9412d75adc",
                packageSize = 1209L,
                releaseNotes = "• Initial release of CoinRadar Crypto & Stock Watcher\n• Real-time price quotes and 7-day ASCII sparkline trend charts\n• Personal portfolio valuation and high/low price trigger alerts",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_fit_track", listOf(
            BotVersion(
                id = "ver_fit_track_100",
                botId = "bot_fit_track",
                version = "1.0.0",
                packageUrl = "packages/bot_fit_track_1.0.0.botpkg",
                sha256 = "ddc0484e75c2f65939c10a3d63bc95de9b5f11eebd014685728fbca6e354a510",
                packageSize = 1210L,
                releaseNotes = "• Initial release of FitTrack Hydration & Workouts\n• Workout session logging (run, gym, bike, yoga)\n• Daily hydration target tracking and weekly calorie summaries",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_parcel_track", listOf(
            BotVersion(
                id = "ver_parcel_track_100",
                botId = "bot_parcel_track",
                version = "1.0.0",
                packageUrl = "packages/bot_parcel_track_1.0.0.botpkg",
                sha256 = "23aae63eb7025fd678a85bd876d690f3f9768a2b014539d2340a637cda8cdcf2",
                packageSize = 1166L,
                releaseNotes = "• Initial release of ParcelTrack Delivery Watcher\n• Multi-courier parcel tracking with custom labels\n• Milestone event timelines and delivery completion alerts",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_rss_notifier", listOf(
            BotVersion(
                id = "ver_rss_notifier_120",
                botId = "bot_rss_notifier",
                version = "1.2.0",
                packageUrl = "packages/bot_rss_1.2.0.botpkg",
                sha256 = "91f367ada04dae04ff3536a8a334d0b18aa411fb51faded21d17ce775663015b",
                packageSize = 1248L,
                releaseNotes = "• RSS 2.0 & Atom 1.0 feed parser with restart deduplication\n• Interactive /feed, /check, and /clearcache commands",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-19T02:00:00Z"
            )
        ))
        put("bot_git_notifier", listOf(
            BotVersion(
                id = "ver_git_notifier_100",
                botId = "bot_git_notifier",
                version = "1.0.0",
                packageUrl = "packages/bot_git_notifier_1.0.0.botpkg",
                sha256 = "de8148aa4edb70b03bf330fd2fc67000f7d8fb5ebbd6d9a41ffe33eaabf4daf1",
                packageSize = 1182L,
                releaseNotes = "• Initial release of GitNotifier Repo Watcher\n• GitHub & GitLab repository monitoring for commits, PRs, and issues\n• CI/CD build pipeline alerts and repository growth statistics",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_cam_sentry", listOf(
            BotVersion(
                id = "ver_cam_sentry_100",
                botId = "bot_cam_sentry",
                version = "1.0.0",
                packageUrl = "packages/bot_cam_sentry_1.0.0.botpkg",
                sha256 = "7ce42a3e6048698231b25bcbd74283414b89577c62aa4185e35aca0571e7ccff",
                packageSize = 1225L,
                releaseNotes = "• Initial release of CamSentry Hardware Security\n• Remote photo capture from front/back phone cameras\n• Flash-assisted photography in darkness and device telemetry stats",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_whisper_box", listOf(
            BotVersion(
                id = "ver_whisper_box_100",
                botId = "bot_whisper_box",
                version = "1.0.0",
                packageUrl = "packages/bot_whisper_box_1.0.0.botpkg",
                sha256 = "826b3fb90acd06cc543e47a7a4f37b2925fd0e926536517e6c203c4668f7243c",
                packageSize = 1197L,
                releaseNotes = "• Initial release of WhisperBox Anonymous Mailbox\n• Shareable public ask links for anonymous feedback\n• Unread question inbox, anonymous replies, and sender moderation",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))
        put("bot_trivia_master", listOf(
            BotVersion(
                id = "ver_trivia_master_100",
                botId = "bot_trivia_master",
                version = "1.0.0",
                packageUrl = "packages/bot_trivia_master_1.0.0.botpkg",
                sha256 = "e4335519ca4da01088e58a7112b92e6611eb480250df4bb9a92d511864e0caa6",
                packageSize = 1202L,
                releaseNotes = "• Initial release of TriviaMaster Daily Brain Quiz\n• Daily 5-question interactive multiple-choice trivia challenge\n• Multi-topic selection, participation streaks, and group leaderboards",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-11-15T00:00:00Z"
            )
        ))

        // Starter / legacy versions
        put("bot_echo", listOf(
            BotVersion(
                id = "ver_echo_100",
                botId = "bot_echo",
                version = "1.0.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_echo_1.0.0.botpkg",
                sha256 = "be89e34e178e8be6643a5e7dc0416d7309f43cb140f627080b33f1002f23cbff",
                packageSize = 1113L,
                releaseNotes = "Initial release of Echo Assistant Bot with structured latency diagnostics.",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-19T01:00:00Z"
            )
        ))
        put("bot_music", listOf(
            BotVersion(
                id = "ver_music_140",
                botId = "bot_music",
                version = "1.4.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_music_1.4.0.botpkg",
                sha256 = "c58161d93bdefe662e41d1c63f5dde7b1343478a9a90d231bd39d20d1d28360a",
                packageSize = 1189L,
                releaseNotes = "• yt-dlp resolver API support\n• Queue seek, skip and volume controls",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-18T10:00:00Z"
            ),
            BotVersion(
                id = "ver_music_100",
                botId = "bot_music",
                version = "1.0.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_music_1.0.0.botpkg",
                sha256 = "8c12f7ae7b3134dc5a27ee7c8c0b5396f6101fe1b97b26d61da498d926f7b391",
                packageSize = 1139L,
                releaseNotes = "Initial release of Music Bot",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-01T10:00:00Z"
            )
        ))
        put("bot_ai", listOf(
            BotVersion(
                id = "ver_ai_120",
                botId = "bot_ai",
                version = "1.2.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_ai_1.2.0.botpkg",
                sha256 = "564fad24f79189a8f9409ad950964eae9b28bbd4a7384fa1a52df3aa5f3cc5f4",
                packageSize = 1167L,
                releaseNotes = "• Gemini 2.5 Flash support with multi-turn memory\n• /summarize, /code, /model, /system & auto-chunking",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-18T12:00:00Z"
            ),
            BotVersion(
                id = "ver_ai_100",
                botId = "bot_ai",
                version = "1.0.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_ai_1.0.0.botpkg",
                sha256 = "f8f130c0af3050937663f80df9a09766aa743dc7a4a99444d25abe4246913b0e",
                packageSize = 1139L,
                releaseNotes = "Initial release of AI Assistant Bot",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-01T10:00:00Z"
            )
        ))
        put("bot_utility", listOf(
            BotVersion(
                id = "ver_util_110",
                botId = "bot_utility",
                version = "1.1.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_utility_1.1.0.botpkg",
                sha256 = "1b4c04c33aec952bca1809257ca0c3dbbc9b588471373264299b0dce49134afe",
                packageSize = 1101L,
                releaseNotes = "• Battery thermal reporting\n• Thread pool introspection",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-15T15:00:00Z"
            ),
            BotVersion(
                id = "ver_util_100",
                botId = "bot_utility",
                version = "1.0.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_utility_1.0.0.botpkg",
                sha256 = "18207df14e1e7d811cf672f930e5a7286dedd331653b47224e8da4cca6f1663b",
                packageSize = 1104L,
                releaseNotes = "Initial release of Utility Bot",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-01T10:00:00Z"
            )
        ))
    }

    fun getBuiltInBots(): List<BotMetadata> = sampleBots

    override suspend fun getBots(): Result<List<BotMetadata>> {
        return Result.success(sampleBots)
    }

    override suspend fun getBotDetails(botId: String): Result<BotMetadata> {
        val bot = sampleBots.find { 
            it.id == botId || 
            (it.id == "bot_commander" && (botId == "bot_commander_flagship" || botId == "tapbot-commander" || botId == "tapbot_commander")) ||
            (it.id == "bot_music_streamer" && (botId == "tapstream-music" || botId == "tapstream_music")) ||
            ((it.id == "bot_exon" || it.id == "bot_1790084493920_n4l8lj") && (botId == "bot_exon" || botId == "bot_1790084493920_n4l8lj"))
        }
        return if (bot != null) {
            Result.success(bot)
        } else {
            Result.failure(NoSuchElementException("Bot not found: $botId"))
        }
    }

    override suspend fun getBotVersions(botId: String): Result<List<BotVersion>> {
        val resolvedId = when (botId) {
            "bot_commander_flagship", "tapbot-commander", "tapbot_commander" -> "bot_commander"
            "tapstream-music", "tapstream_music" -> "bot_music_streamer"
            "bot_exon" -> if (sampleVersions.containsKey("bot_exon")) "bot_exon" else "bot_1790084493920_n4l8lj"
            "bot_1790084493920_n4l8lj" -> if (sampleVersions.containsKey("bot_1790084493920_n4l8lj")) "bot_1790084493920_n4l8lj" else "bot_exon"
            else -> botId
        }
        val list = sampleVersions[resolvedId] ?: emptyList()
        return Result.success(list.filter { it.isPublished })
    }

    override suspend fun getLatestVersion(botId: String): Result<BotVersion?> {
        val resolvedId = when (botId) {
            "bot_commander_flagship", "tapbot-commander", "tapbot_commander" -> "bot_commander"
            "tapstream-music", "tapstream_music" -> "bot_music_streamer"
            "bot_exon" -> if (sampleVersions.containsKey("bot_exon")) "bot_exon" else "bot_1790084493920_n4l8lj"
            "bot_1790084493920_n4l8lj" -> if (sampleVersions.containsKey("bot_1790084493920_n4l8lj")) "bot_1790084493920_n4l8lj" else "bot_exon"
            else -> botId
        }
        val list = sampleVersions[resolvedId]?.filter { it.isPublished }
        return Result.success(list?.firstOrNull())
    }
}
