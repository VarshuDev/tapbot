# TapBot v2.3.0 Multi-Phase Feature & Architecture Upgrade PRD

## 1. Overview
TapBot is an open-source Android application that hosts and runs Telegram bots 24/7 directly on-device via native Kotlin coroutine runtimes, Android Keystore (`AES-256-GCM`) credential storage, and an offline-first Edge Bot Store backed by Cloudflare Workers/D1/R2.

This PRD covers the complete multi-phase upgrade toward **TapBot v2.3.0 (`versionCode = 230`)**:
- **Phase 1 — Core Fixes & Real AI / RSS Runtimes**: Fix install-to-fleet synchronization, enable built-in runtime installation, replace stubbed Gemini AI responses with real Gemini REST inference, and implement a full RSS/Atom broadcaster runtime.
- **Phase 2 — Interactive Inline Keyboards & Upgraded Flagship Runtimes**: Upgrade Telegram Bot API support for inline keyboards and callback queries; upgrade Exon Robot with persistent moderation state, timed bans/mutes, promote/demote, welcome messages, antiflood, and active `/lock` deletion; upgrade Commander with `/tts`, `/wifi`, `/location`, interactive inline HUDs, and scheduled alerts; upgrade TapStream Audio with audio focus, live ticker, `/shuffle`, `/loop`, `/seek`, `/remove`, and Android 14/15 `mediaPlayback` foreground service compliance.
- **Phase 3 — Live Settings, Version Sync & No-Code Custom Bot Builder**: Make all Settings cards interactive via Compose dialogs/sheets, wire boot auto-start preference and multi-bot boot recovery, bump app version to `2.3.0 (230)`, and introduce a No-Code Custom Command & Webhook Bot Builder with Store/Fleet integration.
- **Phase 4 — Package Bundling, Backend Migration & Full Build Verification**: Package valid `.botpkg` archives for all catalog bots into assets, add Cloudflare D1 migration `0008`, run full multi-module Gradle tests, assemble debug & release APKs, publish verified `releases/tapbot-v2.3.0.apk`, and merge to `main`.

---

## 2. User Stories by Phase

### Phase 1: Core Fixes & Real AI / RSS Runtimes

#### US-P1-01: Unify Install-to-Fleet Synchronization & Built-In Runtime Installation
**As a** TapBot user installing bots from the Bot Store,  
**I want** installed bots (including `built-in` bots like Gemini AI Assistant, RSS Broadcaster, and Echo) to install cleanly offline and appear immediately in My Fleet,  
**so that** every bot in the store can be installed, started, stopped, and uninstalled consistently.

**Acceptance Criteria:**
- `BotDetailViewModel` accepts optional `botInstanceManager: BotInstanceManager? = null` (backward compatible) and `TapBotNavHost` passes `app.botInstanceManager`.
- `BotDetailViewModel.installBot()` accepts both `"native_art"` and `"built-in"` runtimes while still rejecting unknown runtimes.
- Installing via `BotDetailViewModel` registers the bot in both `installationManager` and `botInstanceManager`, and starting/stopping from `BotDetailScreen` synchronizes state with `BotInstanceManager` when present.
- Uninstalling via `CatalogViewModel.uninstallBot()` cleans up both `botInstanceManager` and `localBotInstallationManager`.
- `MockCatalogApi` adds optional `gemini_model` / `system_prompt` metadata where helpful while preserving `gemini_api_key` required status and existing catalog expectations.
- Unit tests verify `"built-in"` install works and syncs with `BotInstanceManager`.

#### US-P1-02: Real Gemini AI Client & Upgraded `AIBotRuntime`
**As a** user running Gemini Smart Assistant on my phone,  
**I want** my messages and commands to call the real Google Gemini API using my encrypted Keystore API key with per-chat conversation memory,  
**so that** my Telegram AI bot delivers real, context-aware responses instead of static placeholders.

**Acceptance Criteria:**
- Implement `GeminiApiClient` in `:core:network` using OkHttp against the Gemini `generateContent` endpoint with configurable base URL for testing.
- Upgrade `AIBotRuntime` to load `gemini_api_key`, optional `gemini_model` (default `gemini-2.5-flash`), and optional `system_prompt` from `RuntimeContext.credentials`.
- Maintain per-chat sliding conversation history, support `/ask`, natural PM text replies, `/summarize` (supports replying to a message), `/code`, `/model`, `/system`, `/clear`, `/status`, and chunk responses > 4096 chars.
- Return clear setup instructions if `gemini_api_key` is missing rather than hardcoded fake responses.
- Add unit tests for `GeminiApiClient` and `AIBotRuntime`.

#### US-P1-03: Real RSS / Atom Feed Parser & `RssNotifierBotRuntime`
**As a** user subscribing to news or blog feeds,  
**I want** RSS Channel Broadcaster to periodically parse RSS 2.0 and Atom feeds and broadcast only newly published entries to my target Telegram chat/channel without duplicate spam,  
**so that** I receive reliable real-time updates directly from my phone.

**Acceptance Criteria:**
- Implement JVM-safe `RssFeedParser` using `DocumentBuilderFactory` supporting RSS `<item>` and Atom `<entry>` feeds (`title`, `link`, `guid/id`, `pubDate/updated`, `description`).
- Implement `RssNotifierBotRuntime` reading `target_chat_id` and `feed_url` (supporting multiple URLs), polling on interval (default 15 min), persisting seen GUIDs to JSON on disk so restarts never duplicate alerts, and supporting `/start`, `/help`, `/feeds`, `/addfeed`, `/removefeed`, `/checknow`, `/interval`.
- Register `"rss"`, `"feed"`, and `"notifier"` in `BotRuntimeFactory`.
- Add unit tests for `RssFeedParser`, `RssNotifierBotRuntime`, and `BotRuntimeFactory`.

---

### Phase 2: Inline Keyboards & Upgraded Flagship Runtimes

#### US-P2-01: Telegram Inline Keyboards & `callback_query` Pipeline
**As a** bot developer and user,  
**I want** TapBot runtimes to send interactive inline keyboard buttons and respond to button taps via `callback_query`,  
**so that** Commander, TapStream, Exon, and Custom Bots feel native and responsive inside Telegram.

**Acceptance Criteria:**
- Extend `TelegramUpdate` with `callbackQuery: TelegramCallbackQuery?`, `TelegramMessage` with `newChatMembers`, `sticker`, `photo`, `document`, `caption`, `entities`, and `SendMessagePayload` with `replyMarkup: InlineKeyboardMarkup?`.
- Add `InlineKeyboardMarkup`, `InlineKeyboardButton`, `AnswerCallbackQueryPayload`, `EditMessageTextPayload`, and optional `untilDate` on ban/restrict payloads plus `PromoteChatMemberPayload`.
- Extend `TelegramApiClient` while preserving all existing open method signatures so existing fakes compile unchanged.
- Add unit tests for serialization and API request behavior in `:core:model` and `:core:network`.

#### US-P2-02: Exon Robot 2.0 Persistence, Timed Moderation & Auto-Moderation
**As a** Telegram group admin using Exon Robot,  
**I want** warnings, rules, locks, welcome greetings, and antiflood settings to persist across restarts and actively enforce group rules,  
**so that** group moderation remains reliable even if TapBot restarts.

**Acceptance Criteria:**
- Implement `ExonModerationStore` persisting per-chat `warns`, `rules`, `locks`, `welcomeMessage`, `welcomeEnabled`, and `antifloodLimit` to JSON.
- Add duration parsing (`30s`, `10m`, `2h`, `1d`) for `/tmute`, `/mute <duration>`, and `/tban`.
- Add `/promote`, `/demote`, `/setwelcome`, `/welcome <on|off>`, `/clearwelcome`, `/antiflood <count|off>`, and automatic greeting on `new_chat_members`.
- Actively enforce `/lock` rules (`links`/`url`, `sticker`, `photo`, `media`, `document`, `all`) by calling `deleteMessage` on violating messages.
- Attach inline keyboard category/action buttons on `/help` & `/rules` and handle `callback_query`.
- Preserve all existing `ExonBotRuntimeTest` assertions and add new tests covering persistence, timed moderation, welcome, locks, and callbacks.

#### US-P2-03: Commander Hardware 2.0 (`/tts`, `/wifi`, `/location`, Interactive HUD, Scheduled Alerts)
**As a** phone owner managing my device remotely via Commander,  
**I want** text-to-speech, Wi-Fi/IP details, GPS/last-known location, interactive dashboard buttons, and scheduled reminders,  
**so that** I can inspect and trigger device actions effortlessly with a single tap.

**Acceptance Criteria:**
- Extend `DeviceActionController` with `speakText` (Android `TextToSpeech` + JVM fallback), `getWifiDetails`, and `getLastKnownLocation` (with permission checks & graceful error guidance).
- Declare optional `ACCESS_WIFI_STATE`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` in manifests cleanly.
- Extend `CommanderBotRuntime` with `/tts`, `/wifi` (`/ip`), `/location`, `/schedule <minutes> <msg>`, `/schedules`, `/cancelschedule`, inline HUD buttons on `/status` and `/bots`, and `callback_query` handling.
- Add unit tests in `DeviceActionControllerTest` and `CommanderBotRuntimeTest`.

#### US-P2-04: TapStream Audio 2.0 & Android 14/15 `mediaPlayback` Compliance
**As a** user streaming audio on my Android device via TapStream,  
**I want** `/shuffle`, `/loop`, `/seek`, `/remove`, live playback elapsed progress, Android audio focus handling, interactive player buttons, and Android 14/15 `mediaPlayback` foreground service compliance,  
**so that** music playback stays stable alongside other apps and offers full queue/player control.

**Acceptance Criteria:**
- Add `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission to `app` and `core/runner` manifests and update `BotForegroundService` `foregroundServiceType="dataSync|mediaPlayback"`.
- Upgrade `DeviceAudioPlayer` with `AudioManager` focus management and a live 1-second `elapsedMs` coroutine ticker while playing (cleanly stopped on pause/stop).
- Upgrade `MusicBotRuntime` with `/shuffle`, `/loop [off|one|all]`, `/seek <mm:ss|sec>`, `/remove <index>`, inline player control buttons on `/nowplaying` & `/play`, and `callback_query` support.
- Add unit tests in `DeviceAudioPlayerTest` and `MusicBotRuntimeTest`.

---

### Phase 3: Live Settings, Version Sync (`v2.3.0`) & No-Code Custom Bot Builder

#### US-P3-01: `AppPreferencesRepository`, Interactive Settings Screen, Boot Toggle & `v2.3.0` Sync
**As a** TapBot user opening Settings or rebooting my device,  
**I want** every Settings card to open real interactive controls (boot auto-start toggle, custom Cloudflare catalog URL, emergency Stop All Bots, cache clearing, Keystore inspection, dynamic `v2.3.0 (230)` version display), and reliable boot recovery of all running bots,  
**so that** TapBot has zero dead UI controls and reflects `v2.3.0`.

**Acceptance Criteria:**
- Bump `app/build.gradle.kts` to `versionCode = 230`, `versionName = "2.3.0"`; sync `ApkDeliveryService` fallback & `CommanderBotRuntime` version info.
- Create `AppPreferencesRepository` in `:core:network` backed by JSON/SharedPreferences for `autoStartOnBoot`, `customCatalogUrl`, `wifiOnlyDownloads`, and `diagnosticsExportEnabled`.
- Wire `BootCompletedReceiver` to check `autoStartOnBoot` and restart ALL previously running bots (not just `first()`), and wire `TapBotApplication` to honor `customCatalogUrl`.
- Wire `SettingsScreen.kt` (via `CatalogViewModel` / callbacks or preferences/manager state) with interactive Compose dialogs/sheets for Account, App Preferences (Boot switch, Custom Catalog URL, Emergency Stop All), Security & Encryption, Storage & Cache (real size + Clear Cache + Export Diagnostics), Background Execution, and About TapBot (`v2.3.0 (230)`).
- Add unit tests for `AppPreferencesRepository` and `BootCompletedReceiver`.

#### US-P3-02: No-Code Custom Command & Webhook Bot Engine (`CustomWorkflowBotRuntime`)
**As a** power user who wants custom Telegram slash commands without writing Kotlin,  
**I want** an on-device rule engine supporting custom command replies, template variables, optional HTTP webhooks, and inline buttons persisted locally,  
**so that** I can automate alerts and integrations right from my phone.

**Acceptance Criteria:**
- Add `CustomBotDefinition`, `CommandRule`, and `CustomBotRepository` (JSON persistence in `custom_bots.json`) in `:core:model` / `:core:runner`.
- Support exact/prefix `/command` matching, `{user}`, `{user_id}`, `{chat_id}`, `{args}`, `{date}`, `{webhook_response}` template substitution, optional HTTP GET/POST webhook call via OkHttp, and inline button attachments.
- Implement `CustomWorkflowBotRuntime` and resolve custom bot definitions in `BotRuntimeFactory` + `ServiceLocator`.
- Add unit tests for `CustomBotRepository` and `CustomWorkflowBotRuntime`.

#### US-P3-03: Compose "Bot Builder" Screen & Store/Fleet Integration
**As a** user creating a custom bot inside the TapBot app,  
**I want** a dark OLED "Create Custom Bot" builder UI accessible from Catalog/My Fleet that saves credentials to hardware Keystore and registers the bot in My Fleet immediately,  
**so that** I can build, configure, and start custom bots in under a minute.

**Acceptance Criteria:**
- Create `CustomBotBuilderScreen.kt` and `CustomBotBuilderViewModel.kt` in `:feature:catalog` matching `BotHubTheme`.
- Allow entering Bot Name, Summary, Telegram Bot Token (stored securely in Keystore), and adding/removing `/command -> response / optional webhook URL` rules.
- Saving registers the custom bot in `CustomBotRepository` and `BotInstanceManager` so it appears in My Fleet and Store immediately.
- Wire navigation route in `Screen.kt` & `TapBotNavHost.kt` plus entry button in `CatalogScreen` / `MyBotsScreen`.
- Add unit tests for `CustomBotBuilderViewModel`.

---

### Phase 4: Packaging, Backend Sync, Build Verification & Release

#### US-P4-01: Bundle `.botpkg` Packages & Cloudflare D1 Migration `0008`
**As a** developer and offline/edge user,  
**I want** valid `.botpkg` archives bundled in `app/src/main/assets/packages/` for every catalog bot with matching SHA-256 hashes and D1 migration `0008_v230_catalog_sync.sql`,  
**so that** offline installs and Cloudflare backend metadata match seamlessly.

**Acceptance Criteria:**
- Add packager script generating valid `.botpkg` zips in `app/src/main/assets/packages/` for `bot_exon`, `bot_ai_assistant`, `bot_ai`, `bot_rss_notifier`, `bot_echo`, `bot_utility`, `bot_music`, plus updated `tapbot-commander` and `tapstream-music`.
- Synchronize `MockCatalogApi` URLs/SHA-256 hashes/sizes with bundled packages.
- Create `backend/migrations/0008_v230_catalog_sync.sql`.

#### US-P4-02: End-to-End Test Suite, Release APK Build & Merge to `main`
**As a** maintainer shipping TapBot `v2.3.0`,  
**I want** all unit tests across every Gradle module to pass, both debug and release APKs to build cleanly, `releases/tapbot-v2.3.0.apk` to be archived with verified SHA-256 metadata, and `ralph/tapbot-v230` cleanly merged into `main`.

**Acceptance Criteria:**
- Run `./gradlew.bat test` across all modules with zero failures.
- Run `./gradlew.bat assembleDebug` and `./gradlew.bat assembleRelease` cleanly.
- Copy verified APK to `releases/tapbot-v2.3.0.apk` and update `releases/README.md` with accurate SHA-256 and size.
- Verify all `prd.json` stories show `passes: true` and `progress.txt` is finalized, then merge `ralph/tapbot-v230` -> `main`.
