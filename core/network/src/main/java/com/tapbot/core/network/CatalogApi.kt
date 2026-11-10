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
            id = "bot_rss_notifier",
            name = "RSS Channel Broadcaster",
            summary = "Periodically checks your favorite RSS/Atom feeds and broadcasts new articles to your Telegram channel.",
            description = "Runs on your Android device 24/7. Configure your target Telegram Channel ID and feed URL. The bot tracks published items in local storage with deduplication and pushes instant alerts with rich formatting.",
            author = "OpenFeed Community",
            version = "1.2.0",
            iconUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200",
            category = "News & Media",
            tags = listOf("rss", "channel", "news", "automation"),
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
                packageUrl = "https://assets.tapbot.internal/packages/bot_rss_1.2.0.botpkg",
                sha256Checksum = "91f367ada04dae04ff3536a8a334d0b18aa411fb51faded21d17ce775663015b",
                runtimeType = "built-in",
                entryClass = "com.tapbot.core.runner.runtime.RssNotifierBotRuntime",
                packageSizeBytes = 1248L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),
        BotMetadata(
            id = "bot_ai_assistant",
            name = "Gemini Smart Assistant",
            summary = "Conversational AI assistant powered by Google Gemini running directly through your personal Telegram account.",
            description = "Turns your personal Telegram bot into a generative AI assistant. Supports multi-turn chats, code analysis, and summarization right in Telegram. Your Gemini API key and Telegram token remain encrypted in your device Keystore.",
            author = "TapBot Labs",
            version = "2.1.0",
            iconUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=200",
            category = "AI & Productivity",
            tags = listOf("ai", "gemini", "assistant", "productivity"),
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
                packageUrl = "https://assets.tapbot.internal/packages/bot_ai_assistant_2.1.0.botpkg",
                sha256Checksum = "44c098853cd5d46484141cefa70e3bbb763915e30c71848bd67316cb8ff9c185",
                runtimeType = "built-in",
                entryClass = "com.tapbot.core.runner.runtime.AIBotRuntime",
                packageSizeBytes = 1212L
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE")
        ),
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
        ),
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
        )
    )

    private val sampleVersions = mapOf(
        "bot_commander" to listOf(
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
        ),
        "bot_commander_flagship" to listOf(
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
        ),
        "bot_echo" to listOf(
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
        ),
        "bot_rss_notifier" to listOf(
            BotVersion(
                id = "ver_rss_notifier_120",
                botId = "bot_rss_notifier",
                version = "1.2.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_rss_1.2.0.botpkg",
                sha256 = "91f367ada04dae04ff3536a8a334d0b18aa411fb51faded21d17ce775663015b",
                packageSize = 1248L,
                releaseNotes = "• RSS 2.0 & Atom 1.0 feed parser with restart deduplication\n• Interactive /feed, /check, and /clearcache commands",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-19T02:00:00Z"
            )
        ),
        "bot_ai_assistant" to listOf(
            BotVersion(
                id = "ver_ai_assistant_210",
                botId = "bot_ai_assistant",
                version = "2.1.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_ai_assistant_2.1.0.botpkg",
                sha256 = "44c098853cd5d46484141cefa70e3bbb763915e30c71848bd67316cb8ff9c185",
                packageSize = 1212L,
                releaseNotes = "• Real Google Gemini API integration with per-chat conversation history\n• /ask, /summarize, /code, /model, /system, /clear and 4096-char auto chunking",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-10-01T00:00:00Z"
            )
        ),
        "bot_music_streamer" to listOf(
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
        ),
        "bot_1790084493920_n4l8lj" to listOf(
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
                publishedAt = "2026-09-22T00:00:00Z"
            )
        ),
        "bot_music" to listOf(
            BotVersion(
                id = "ver_music_140",
                botId = "bot_music",
                version = "1.4.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_music_1.4.0.botpkg",
                sha256 = "c58161d93bdefe662e41d1c63f5dde7b1343478a9a90d231bd39d20d1d28360a",
                packageSize = 1189L,
                releaseNotes = "• Added high-resolution audio streaming with AudioFocus\n• /shuffle, /loop, /seek, /remove and inline playback controls",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-20T10:00:00Z"
            ),
            BotVersion(
                id = "ver_music_100",
                botId = "bot_music",
                version = "1.0.0",
                packageUrl = "https://assets.tapbot.internal/packages/bot_music_1.0.0.botpkg",
                sha256 = "8c12f7ae7b3134dc5a27ee7c8c0b5396f6101fe1b97b26d61da498d926f7b391",
                packageSize = 1139L,
                releaseNotes = "Initial release of Music Bot for Android",
                minimumAppVersion = 1,
                minimumRuntimeVersion = "1.0.0",
                isPublished = true,
                publishedAt = "2026-09-01T10:00:00Z"
            )
        ),
        "bot_ai" to listOf(
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
        ),
        "bot_utility" to listOf(
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
        )
    )

    fun getBuiltInBots(): List<BotMetadata> = sampleBots

    override suspend fun getBots(): Result<List<BotMetadata>> {
        return Result.success(sampleBots)
    }

    override suspend fun getBotDetails(botId: String): Result<BotMetadata> {
        val bot = sampleBots.find { 
            it.id == botId || 
            (it.id == "bot_commander" && (botId == "bot_commander_flagship" || botId == "tapbot-commander" || botId == "tapbot_commander")) ||
            (it.id == "bot_music_streamer" && (botId == "tapstream-music" || botId == "tapstream_music"))
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
            else -> botId
        }
        val list = sampleVersions[resolvedId] ?: emptyList()
        return Result.success(list.filter { it.isPublished })
    }

    override suspend fun getLatestVersion(botId: String): Result<BotVersion?> {
        val resolvedId = when (botId) {
            "bot_commander_flagship", "tapbot-commander", "tapbot_commander" -> "bot_commander"
            "tapstream-music", "tapstream_music" -> "bot_music_streamer"
            else -> botId
        }
        val list = sampleVersions[resolvedId]?.filter { it.isPublished }
        return Result.success(list?.firstOrNull())
    }
}
