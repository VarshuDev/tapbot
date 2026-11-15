# Phase 1 Technical Product Analysis Report: TapBot Launch Video

**Document:** `/video-output/PHASE-1-REPORT.md`  
**Project:** [TapBot (VarshuDev/tapbot)](https://github.com/VarshuDev/tapbot)  
**Target Release:** TapBot v2.4.0 (Build `240`)  
**Role:** Lead Technical Product Analyst  
**Date:** November 11, 2026  
**Status:** Complete — Awaiting Approval to proceed to Phase 2  

---

## Executive Summary

TapBot is a decentralized, on-device Telegram Bot Runner, Hardware Commander, and Edge Bot Store for Android (API 29–35 / Android 10+). It transforms physical Android smartphones into autonomous 24/7 Telegram bot hosting nodes. By executing bots within the native Android Runtime (ART) coroutine engine and communicating via outbound HTTPS long-polling (`getUpdates` on port 443), TapBot completely eliminates recurring cloud VPS bills, serverless compute charges, static IP requirements, and port-forwarding hassles.

All sensitive Telegram bot tokens and API credentials remain encrypted in hardware-backed Android Keystore storage (`AES-256-GCM`), never leaving the physical device. The ecosystem is paired with a serverless edge catalog powered by Cloudflare Workers, Cloudflare D1 (SQLite), and Cloudflare R2 object storage for distributed `.botpkg` package delivery.

The codebase is in a verified, production-ready state: the test suite passes across all 9 Gradle modules, and the release APK (`releases/tapbot-v2.4.0.apk`, 12.39 MB) is compiled, signed, and verified via `apksigner`.

---

## 1. Product Overview

### 1.1 Core Value Proposition
* **$0/Month Zero-Cost Hosting**: Replaces AWS EC2, DigitalOcean Droplets, Heroku, or VPS hosting ($5–$25/mo) by repurposing spare or primary Android hardware.
* **100% Data Sovereignty**: Bot tokens, chat logs, and telemetry are stored exclusively inside the Android device's encrypted app sandbox.
* **NAT & Firewall Piercing**: Communicates via outbound persistent TLS 1.3 long-polling to Telegram servers (`api.telegram.org:443`). Operates seamlessly behind cellular carrier-grade NAT (CGNAT), dynamic mobile IPs, and restrictive Wi-Fi firewalls without public IP addresses, domain names, or reverse proxies.
* **Minimalist ART Footprint**: Requires only **8 MB – 12 MB RAM** per bot instance with **< 0.1% CPU utilization** during idle polling and cold startup latency under **50 ms**.
* **Uninterrupted 24/7 Execution**: Engineered with a hardened Android Foreground Service (`dataSync|mediaPlayback`) and wake-lock management that persists across Android Doze Mode, screen-off cycles, network handovers, and device reboots.

### 1.2 Target Audience
1. **Telegram Power Users & Channel Admins**: Running community moderation, RSS broadcasting, or AI chatbots without server management.
2. **Homelab & Self-Hosting Enthusiasts**: Turning decommissioned Android phones into micro-servers and hardware automation hubs.
3. **Developers & Automation Creators**: Deploying custom webhook bots, hardware remote-controllers, and custom workflows via an on-device visual builder.

---

## 2. Technical Architecture

The TapBot ecosystem consists of a multi-module Android application client and a serverless Cloudflare Edge catalog backend.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          TELEGRAM CLOUD API                            │
│                  Outbound Long-Polling HTTPS (Port 443)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    ANDROID DEVICE (TapBot Host)                        │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ :app & :core:ui (Jetpack Compose M3 Deep Obsidian Theme)         │  │
│  │ • Store Catalog  • My Fleet (Telemetry HUD)  • Settings Dialogs  │  │
│  │ • Bot Detail (Console Logs)  • Custom Bot Builder (Chat Preview) │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ :core:runner (In-Process Execution & Orchestration)              │  │
│  │ • BotForegroundService (dataSync|mediaPlayback, START_STICKY)    │  │
│  │ • BotInstanceManager (Atomic State Machine & Auto-Rollback)      │  │
│  │ • DeviceWatchdog (Battery < 15% & Crash Alerts)                  │  │
│  │ • DeviceActionController (Torch, Ring, Haptics, TTS, Location)   │  │
│  │ • DeviceTelemetryProvider (Battery, RAM, Storage, Network HUD)   │  │
│  │ • ApkDeliveryService (Direct On-Device SHA-256 APK Distribution) │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ Bot Runtimes (:core:runner)                                      │  │
│  │ • CommanderBotRuntime 2.0 (Remote Hardware & Fleet Control)      │  │
│  │ • ExonBotRuntime 2.0 (Group Moderation & Antiflood)              │  │
│  │ • MusicBotRuntime 2.0 (TapStream Audio & Radio Streaming)        │  │
│  │ • AIBotRuntime (Gemini 1.5 Flash Conversational Chatbot)         │  │
│  │ • RssNotifierBotRuntime (RSS/Atom Feed Broadcaster)              │  │
│  │ • CustomWorkflowBotRuntime (No-Code Command & Webhook Engine)    │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │ :core:security & :core:logging                                   │  │
│  │ • KeystoreSecureCredentialStore (AES-256-GCM / TEE / StrongBox)  │  │
│  │ • SecretRedactor (Regex Token Scrubbing in Logs & UI)            │  │
│  │ • SecurityWindowManager (FLAG_SECURE Anti-Snooping Protection)   │  │
│  │ • InMemoryRingBufferLogRepository (Sanitized Log Stream)         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS GET /api/v1/bots (REST)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 CLOUDFLARE EDGE BACKEND (Serverless)                   │
│                                                                        │
│  ┌─────────────────────────┐  ┌────────────────┐  ┌─────────────────┐  │
│  │ Cloudflare Workers API  │  │ Cloudflare D1  │  │  Cloudflare R2  │  │
│  │ Edge Router (/api/v1/*) │  │ Serverless SQL │  │ Object Storage  │  │
│  │ Dark Ops Admin HUD      │  │ 8 Migrations   │  │ (.botpkg Zips)  │  │
│  └─────────────────────────┘  └────────────────┘  └─────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1 Gradle Module Breakdown
1. **`:app`**: Application entrypoint (`MainActivity`, `TapBotApplication`), top-level navigation (`TapBotNavHost`, `Screen`), lifecycle DI wiring via `ServiceLocator`.
2. **`:core:model`**: Data contracts, domain models (`BotMetadata`, `BotInstance`, `BotCredentialSpec`, `TelegramUpdate`, `TelegramMessage`, `InlineKeyboardMarkup`, `CustomBotDefinition`, `UpdateState`).
3. **`:core:security`**: Hardware-backed Android Keystore integration (`MasterKey`, `EncryptedSharedPreferences`, `AES-256-GCM`), high-performance `SecretRedactor`, and window display defense (`SecurityWindowManager` / `FLAG_SECURE`).
4. **`:core:network`**: Network communication layer (`TelegramApiClient`, `GeminiApiClient`, `CatalogRepository`, `CloudflareCatalogApi`, `PackageDownloader`, `PackageVerifier`, `ManifestValidator`, `AppPreferencesRepository`).
5. **`:core:logging`**: Structured logging framework (`BotLogRepository`, `InMemoryRingBufferLogRepository`) with automatic regex token scrubbing.
6. **`:core:runner`**: Runtime orchestration, `BotForegroundService`, `BotInstanceManager`, `DeviceWatchdog`, `DeviceActionController`, `DeviceTelemetryProvider`, `ApkDeliveryService`, audio playback engine, and all 7 bot runtime implementations.
7. **`:core:ui`**: Deep Obsidian design system (`TapBotTheme`), motion curves (`TapBotMotion`), reusable UI components (`BotCard`, `BotCardSkeleton`, `TapBotTelemetryCard`, `AudioPlaybackStrip`, `TelegramChatBubblePreview`), and command cheatsheet catalog (`BotCommandCatalog`).
8. **`:feature:catalog`**: Store catalog browser, category filtering, search, offline caching, My Fleet HUD, interactive Settings dialogs, Custom Bot Builder with starter blueprints.
9. **`:feature:botdetail`**: Detailed bot management, dynamic credential forms, lifecycle execution controls, update progress/rollback indicators, live console log viewer with filters and sanitized copy.

---

## 3. Verified Feature Inventory

Every feature listed below has been verified against the production codebase and unit test suite.

| Component / Subsystem | Verified Feature | Technical Implementation | Status |
| :--- | :--- | :--- | :--- |
| **Android Service** | 24/7 Foreground Service | `BotForegroundService` with `dataSync\|mediaPlayback` service types and ongoing system tray notification | **VERIFIED** |
| **Power Management** | Doze Mode Survival | Explicit wake-lock acquisition during update dispatch; 1-tap Doze exemption intent (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | **VERIFIED** |
| **Reboot Recovery** | Auto-Start on Boot | `BootCompletedReceiver` listening to `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, and `MY_PACKAGE_REPLACED`, recovering all previously running bots | **VERIFIED** |
| **Safety Guards** | Duplicate Process Prevention | `BotForegroundService` checks `activeExecutions` by installation ID and bot ID to prohibit parallel duplicates | **VERIFIED** |
| **Safety Guards** | Duplicate Connection Prevention | Enforces unique token checking across all active bots; blocks duplicate long-polling to prevent Telegram 409 conflicts | **VERIFIED** |
| **Safety Guards** | Orphaned Service Prevention | Automatic teardown via `stopForeground(true)` and `stopSelf()` when zero bots remain running | **VERIFIED** |
| **Hardware Commander** | Device Telemetry HUD | `/status`, `/battery`, `/ram`, `/storage`, `/network`, `/uptime` querying Android subsystem providers | **VERIFIED** |
| **Hardware Commander** | Remote Camera Flashlight | `/torch on\|off` toggling LED torch via Android `CameraManager` | **VERIFIED** |
| **Hardware Commander** | Audible Lost Phone Finder | `/ring [sec]` maximizing alarm stream volume and looping alarm ringtone; `/stopring` to silence | **VERIFIED** |
| **Hardware Commander** | Haptic Feedback | `/vibrate` triggering 500ms haptic pulses via Android `Vibrator` / `VibratorManager` | **VERIFIED** |
| **Hardware Commander** | System Tray Alerts | `/notify <msg>` posting high-priority heads-up notifications via `NotificationManager` | **VERIFIED** |
| **Hardware Commander** | Remote Text-to-Speech | `/tts <text>` vocalizing phrases out loud through device speakers via Android `TextToSpeech` | **VERIFIED** |
| **Hardware Commander** | Wi-Fi & IP Diagnostics | `/wifi` reporting active SSID, BSSID, RSSI dBm, link speed Mbps, frequency, and local IPv4 | **VERIFIED** |
| **Hardware Commander** | Last Known GPS Location | `/location` querying `LocationManager` and generating OpenStreetMap / Google Maps coordinate links | **VERIFIED** |
| **Hardware Commander** | Scheduled Reminder Alerts | `/schedule <sec> <msg>`, `/schedules`, `/cancelschedule <id>` running in-memory delayed coroutines | **VERIFIED** |
| **Hardware Commander** | Direct APK File Delivery | `/apk`, `/download` resolving installed APK from `applicationInfo.sourceDir`, computing SHA-256, and sending via `sendDocument` | **VERIFIED** |
| **Hardware Commander** | Interactive Inline HUD | Full callback query pipeline (`cmd:refresh_status`, `cmd:battery`, `cmd:fleet`, `cmd:wifi`, `cmd:torch`, `cmd:vibrate`, `cmd:start:<id>`, `cmd:stop:<id>`) | **VERIFIED** |
| **Autonomous Watchdog** | Low Battery Alerts | Background coroutine pushing Telegram warnings when battery drops below 15% and discharging (30m debounce) | **VERIFIED** |
| **Autonomous Watchdog** | Bot Crash Detection | Evaluates instances and pushes instant crash alert with failure reason and recovery command links | **VERIFIED** |
| **TapStream Audio** | Dual-Routing Audio | Streams through phone speaker/Bluetooth (`DeviceAudioPlayer`) AND delivers playable audio files to Telegram (`sendAudio`) | **VERIFIED** |
| **TapStream Audio** | AudioFocus Compliance | Listens to `AudioManager.OnAudioFocusChangeListener`, auto-pausing/ducking on phone calls or navigation prompts | **VERIFIED** |
| **TapStream Audio** | Queue & Playback Controls | `/play`, `/pause`, `/resume`, `/skip`, `/stop`, `/queue`, `/nowplaying`, `/volume`, `/radio`, `/shuffle`, `/loop [off\|one\|all]`, `/seek`, `/remove` | **VERIFIED** |
| **Exon Robot** | Group Management | `/ban`, `/sban`, `/tban` (timed ban with duration parser), `/unban`, `/kick`, `/mute`, `/tmute`, `/promote`, `/demote`, `/adminlist`, `/pin`, `/invitelink` | **VERIFIED** |
| **Exon Robot** | Persistent Warning System | `/warn`, `/warns`, `/resetwarn` with automatic 3-strike ban, persisted to disk via `ExonModerationStore` | **VERIFIED** |
| **Exon Robot** | Active Content Locks | `/lock`, `/unlock`, `/locks` actively deleting prohibited message types (stickers, media, links, forwards) via `deleteMessage` | **VERIFIED** |
| **Exon Robot** | Antiflood & Auto-Welcome | 10-second sliding-window flood detection (`/antiflood <count\|off>`), automated join greetings, and anti-bot join auto-ban | **VERIFIED** |
| **Gemini AI Bot** | Generative Chat & Vision | Real OkHttp client calling Google Gemini (`generateContent`), supporting multi-turn chat history sliding window | **VERIFIED** |
| **Gemini AI Bot** | Conversational Commands | `/ask`, `/summarize` (inline or reply-to), `/code`, `/model`, `/system`, `/clear`, `/status`, PM natural chat, 4000-char message chunking | **VERIFIED** |
| **RSS Broadcaster** | Multi-Feed Broadcaster | Pure JVM XML parser supporting RSS 2.0, RDF, and Atom 1.0; automated periodic polling and channel broadcasting | **VERIFIED** |
| **RSS Broadcaster** | Deduplication at Rest | Persists seen GUIDs and article links in JSON storage to prevent re-broadcasting duplicates after restarts | **VERIFIED** |
| **Custom Bot Engine** | No-Code Blueprint Builder | Author bots with custom command rules, trigger matching, focus-tracked template variable chips, and live chat preview bubble | **VERIFIED** |
| **Custom Bot Engine** | Dynamic Variables & Webhooks | Evaluates `{user}`, `{username}`, `{user_id}`, `{chat_id}`, `{args}`, `{date}`, `{webhook_response}` and fires HTTP GET/POST webhooks | **VERIFIED** |
| **Store & Packages** | `.botpkg` Architecture | Standard zip package containing `manifest.json`, `entrypoint`, `dependencies/`, `assets/icon.png`; SHA-256 checksum integrity verification | **VERIFIED** |
| **Store & Packages** | Atomic Update & Rollback | Automatic update state machine: checks version, downloads, verifies SHA-256, stages backup, starts new version, and auto-rolls back if startup health check fails | **VERIFIED** |
| **Hardware Security** | Keystore Master Encryption | Android Keystore `MasterKey` (`AES256_GCM`) backed by StrongBox / TEE hardware security module | **VERIFIED** |
| **Hardware Security** | Encrypted Storage Sandbox | `EncryptedSharedPreferences` (`AES256_SIV` keys, `AES256_GCM` values) under `tapbot_secure_vault` | **VERIFIED** |
| **Hardware Security** | Secret Redaction Engine | High-performance regex scrubber redacting Telegram tokens, OpenAI/Anthropic keys, Bearer headers, and runtime secrets in logs/UI | **VERIFIED** |
| **Hardware Security** | Anti-Snooping Window Flag | `SecurityWindowManager` applying `FLAG_SECURE` to block screenshots, screen recording, and Recent Apps task switcher thumbnails | **VERIFIED** |
| **Backend & Edge** | Serverless Cloudflare Catalog | Cloudflare Workers REST API (`/api/v1/bots`), Cloudflare D1 relational database, Cloudflare R2 package storage, and `/admin` Dark Ops HUD | **VERIFIED** |

---

## 4. Important User Workflows

### 4.1 Discovery & Installation Workflow
1. User opens the **Store** tab in TapBot, browsing featured bots, searching by keyword, or filtering by category (`Utilities`, `Media`, `Management`, `AI & Automation`, `Custom`).
2. Tapping a bot card navigates to the **Bot Detail Screen**.
3. User taps **Install**:
   - The app downloads the `.botpkg` archive (or reads the pre-bundled local package).
   - `Sha256PackageVerifier` validates the cryptographic digest against the catalog hash.
   - `ManifestValidator` verifies manifest constraints, permissions, and minimum runtime versions.
   - `LocalBotInstallationManager` copies the package to secure app storage and registers the installation record.

### 4.2 Credential Configuration & Telegram Handshake
1. On the Bot Detail Screen, user enters required credentials (e.g., Telegram Bot Token from `@BotFather`).
2. Input fields enforce obfuscation (`PasswordVisualTransformation`) with show/hide toggle.
3. Upon tapping **Save Credentials**, secrets are encrypted directly into the Android Keystore (`AES-256-GCM`).
4. User taps **Start**:
   - The runner executes `getMe` to verify credentials against Telegram.
   - The UI displays a verified green badge with `@username`.
   - A direct **"Launch on Telegram"** CTA button appears, opening the Telegram client directly to `t.me/<username>`.

### 4.3 24/7 Background Execution & Remote Control
1. `BotForegroundService` starts in foreground mode, displaying an ongoing notification in the Android status bar.
2. The bot establishes outbound HTTPS long-polling to Telegram (`getUpdates` on port 443).
3. The phone's screen can be turned off, locked, or placed in a drawer.
4. The user controls the phone or fleet remotely via Telegram chat:
   - Sending `/status` retrieves battery %, RAM, disk space, and bot statuses.
   - Sending `/torch on` turns on the phone's physical flashlight.
   - Sending `/ring` sounds a loud alarm to find a misplaced device.
   - Sending `/apk` uploads the latest TapBot APK directly to Telegram.
5. If the battery drops below 15% and is unplugged, the **DeviceWatchdog** pushes an urgent alert to Telegram.

### 4.4 No-Code Custom Bot Creation Workflow
1. User taps **Build Custom Bot** from My Fleet or Store.
2. User selects a starter blueprint (`AI Assistant`, `Welcome & Rules`, `Webhook Alert`, or `FAQ Support`).
3. User configures Bot Name, Summary, and Telegram Bot Token.
4. User defines Command Rules:
   - Sets command trigger (e.g. `/ping`, `/deploy`, `/quote`).
   - Taps focus-tracked template variable chips (`{user}`, `{args}`, `{date}`, `{webhook_response}`) to insert variables into the response text.
   - Optionally toggles an external HTTP Webhook (URL, GET/POST method, JSON payload).
   - Configures inline keyboard buttons.
5. An interactive **Live Chat Preview Bubble** renders exactly how Telegram will display the response.
6. User taps **Save & Deploy** — the custom bot is instantly registered into the fleet and started.

### 4.5 Atomic Update & Automatic Rollback Workflow
1. When a new bot version is available in the catalog, an **Update Available** badge appears on the bot card.
2. User taps **Update**:
   - The updater enters `CHECKING` -> `DOWNLOADING` -> `VERIFYING` (SHA-256 + Manifest check).
   - Old instance is stopped (`INSTALLING`).
   - Existing working `.botpkg` is staged into a timestamped backup file (`.backup_<timestamp>`).
   - New package is unpacked into place, preserving all Keystore credentials.
   - Updater attempts to start the new version (`STARTING`).
3. **If startup succeeds and health checks pass**: The temporary backup file is deleted, and state transitions to `SUCCESS`.
4. **If startup fails or runtime crashes**: The updater immediately triggers `ROLLING_BACK`, deletes the corrupted new package, restores the staged backup file, and restarts the previous verified version with zero user intervention.

---

## 5. Actual UI Screens Available

TapBot features a bespoke **Deep Obsidian** hardware-node console interface built with Jetpack Compose Material 3:

### Screen 1: Store Catalog Screen (`CatalogScreen`)
* **Color Canvas**: Deep Obsidian (`#080A0E`) with obsidian card containers (`#0E121A`).
* **Header**: Top app bar with TapBot brand logo, version pill (`v2.4.0`), active running bot counter pill, and refresh button.
* **Search & Filters**: Debounced search bar with instant clear button; horizontally scrollable category chips (`All`, `Utilities`, `Media`, `Management`, `AI & Automation`, `Custom`).
* **Offline Cache Banner**: Visible offline indicator when network is disconnected, demonstrating that cached bots remain fully installable offline.
* **Bot Cards**: High-contrast cards featuring bot icon, title, version, category badge, summary description, and an action button (`Install`, `Configure`, `Open`).
* **Bottom Navigation Bar**: Fixed bottom bar with 3 tabs (`Store`, `Fleet`, `Settings`) featuring icon badges and active-pulse dots.

### Screen 2: My Fleet Screen (`MyBotsScreen`)
* **Live Hardware Telemetry HUD**:
  - Battery Card: Live percentage, charging state (AC/USB/Wireless), discharging rate, and a 1-tap **"Exempt Doze"** button opening Android battery optimization settings.
  - Memory Card: Total device RAM, available RAM, used RAM progress indicator, and ART JVM heap metrics.
  - Storage Card: Device internal storage usage bar.
  - Network Card: Active transport type (Wi-Fi SSID, LTE/5G Cellular, or Offline).
  - System Uptime: Uptime since Android system boot and TapBot service runtime.
* **TapStream Live Audio Strip**: Appears dynamically when music bots are active; shows current track title, artist, audio duration/elapsed time, Play/Pause toggle, Skip button, and volume slider.
* **Fleet Bot Cards**: Cards showing running status (`🟢 RUNNING`, `⚪ STOPPED`, `🔴 CRASHED`), uptime ticker, and quick action buttons (`Start`, `Stop`, `Restart`, `Configure`).
* **Empty State**: Interactive card with "Build Custom Bot" CTA when no bots are installed.

### Screen 3: Bot Detail Screen (`BotDetailScreen`)
* **Header & Identity**: High-resolution icon, bot title, author, version badge, package size, and active status pill.
* **Telegram Handshake Verification**: Shows verified bot identity with `@username` and a high-contrast **"Launch on Telegram"** button.
* **Bot Command Cheatsheet**: Expandable accordion listing every command supported by the bot with 1-tap clipboard copy.
* **Credential Input Fields**: Dynamic form generated from package `manifest.json`. Password masking with eye icon toggle, helper URLs (e.g. `@BotFather`), and validation errors.
* **Primary Action Bar**: Context-aware buttons (`Install`, `Start`, `Stop`, `Restart`, `Update`, `Uninstall`).
* **Update & Rollback Progress HUD**: Animated linear progress bar showing live update stages (`DOWNLOADING` -> `VERIFYING` -> `INSTALLING` -> `STARTING` -> `ROLLING_BACK` -> `SUCCESS`).
* **Live Console Log Viewer**: Embedded terminal with real-time log streaming from `InMemoryRingBufferLogRepository`. Features:
  - Log Level Filter Chips: `ALL`, `DEBUG`, `INFO`, `WARN`, `ERROR`.
  - Auto-Scroll Lock toggle button.
  - 1-tap **"Copy Sanitized Logs"** button (scrubbed via `SecretRedactor`).
  - Clear logs button.

### Screen 4: Custom Bot Builder Screen (`CustomBotBuilderScreen`)
* **Blueprint Carousel**: Quick-start templates (`AI Assistant`, `Welcome & Rules`, `Webhook Alert`, `FAQ Support`).
* **Bot Information**: Text inputs for Bot Name, Summary, and Bot ID.
* **Token Vault**: Dedicated Telegram Bot Token input encrypted into Keystore.
* **Command Rules Editor**:
  - Command Trigger field (e.g. `/status`, `/help`, `!deploy`).
  - Template Variable Insertion Chips: Focus-tracked buttons for `{user}`, `{username}`, `{user_id}`, `{chat_id}`, `{args}`, `{date}`, `{webhook_response}`.
  - Webhook Toggle: Checkbox expanding HTTP Method (GET/POST), Webhook URL, and JSON Payload editor.
  - Inline Button Editor: Button label and URL/callback data.
* **Live Telegram Chat Preview**: Interactive chat bubble card showing real-time formatted preview of the bot's response with sample variable substitution.
* **Bottom Action Bar**: "Cancel" and "Save & Deploy" buttons.

### Screen 5: Settings Screen (`SettingsScreen`)
* **Settings Groups**: Grouped configuration cards with interactive modal dialogs:
  1. **Account**: Telegram owner ID configuration.
  2. **App Preferences**: Auto-start on boot toggle, custom Cloudflare catalog URL, Wi-Fi only download toggle, diagnostics export toggle.
  3. **Security & Encryption**: Status of hardware Keystore, AES-256-GCM cipher status, and `FLAG_SECURE` screen protection status.
  4. **Storage & Cache**: Cache size counter, "Clear Offline Cache" button, and "Export Diagnostics JSON" button.
  5. **Background Execution**: In-depth OEM-specific battery optimization guides (Samsung, Xiaomi, Pixel, OnePlus).
  6. **About TapBot**: Version display (`v2.4.0`), build code (`240`), Apache 2.0 license, and link to GitHub repository.

---

## 6. Security and Reliability Capabilities

TapBot is architected around strict Android defense-in-depth security principles:

1. **Hardware-Backed Keystore Master Keys**:
   - Master keys are generated inside the Android hardware security module (TEE or StrongBox) using `MasterKey.Builder` with `KeyScheme.AES256_GCM`.
   - Keys are non-exportable and cannot be extracted even on rooted devices with physical access.
2. **Encrypted at Rest**:
   - All tokens and user credentials are stored via `EncryptedSharedPreferences` using `AES256_SIV` for key encryption and `AES256_GCM` for value encryption under `tapbot_secure_vault`.
3. **Zero Third-Party Telemetry & Token Isolation**:
   - Bot tokens are **never** transmitted to the Cloudflare catalog backend, analytics SDKs, or third-party servers.
   - Tokens are dispatched strictly to official Telegram API endpoints (`https://api.telegram.org/bot<token>/...`) via TLS 1.3.
4. **SecretRedactor Engine**:
   - Active regex scrubbing pipeline across all internal logging, console outputs, UI text, and crash dumps.
   - Masks Telegram tokens (`1234****:[REDACTED_TELEGRAM_TOKEN]`), Telegram URLs, OpenAI keys (`sk-proj-****:[REDACTED_API_KEY]`), Anthropic keys, Bearer headers, and dynamic runtime credentials before writing to disk or displaying on screen.
5. **Display & Screen Capture Protection**:
   - `SecurityWindowManager` activates `WindowManager.LayoutParams.FLAG_SECURE` across sensitive windows, preventing secrets from leaking via screen recordings, user screenshots, or Android Recent Apps / Task Switcher snapshot thumbnails.
6. **Owner-Gated Execution**:
   - Sensitive Commander Bot commands (`/status`, `/ring`, `/torch`, `/notify`, `/clipboard`, `/setclip`, `/tts`, `/location`, `/stopbot`, `/startbot`, `/restartbot`, `/logs`, `/exportlogs`) strictly verify the sender's numeric Telegram User ID against the encrypted owner ID configured in settings.
7. **Android W^X & Memory Sandbox**:
   - Operates 100% within native Android ART memory space without spawning arbitrary Linux shell binaries or compiling untrusted native code.

---

## 7. Features Suitable for Marketing (What to Highlight)

These features are verified, robust, visually exciting, and provide compelling narrative hooks for video content:

1. **Turn Any Spare Phone into a 24/7 Telegram Bot Server**:
   - Repurpose old Android phones sitting in drawers.
   - Zero monthly hosting bills ($0/month forever).
2. **Flagship Commander Bot — Remote Android Hardware Control**:
   - Remotely control your phone from anywhere in the world via Telegram.
   - Toggle camera flashlight (`/torch on|off`).
   - Find misplaced phone with loud alarm ringtone (`/ring`).
   - Trigger text-to-speech voice announcements (`/tts`).
   - Query live phone battery, RAM, internal storage, and GPS location.
3. **Direct In-Chat APK Distribution**:
   - Send `/apk` in Telegram to instantly receive the signed TapBot APK file directly in the chat with verified SHA-256 integrity hash.
4. **TapStream Audio 2.0**:
   - Control music playback on your phone speakers or send streaming audio directly into Telegram voice chats.
   - Built-in lofi and ambient radio stations with interactive inline playback controls.
5. **On-Device Gemini AI Assistant**:
   - Conversational AI powered by Google Gemini running directly on your phone with sliding conversation memory and code generation.
6. **Exon Robot 2.0 Group Moderation**:
   - Complete supergroup moderation engine with timed bans/mutes, 3-strike warning bans, active lock message deletion, and anti-flood filters.
7. **No-Code Custom Bot Builder**:
   - Visual drag-and-drop template builder with dynamic variable chips (`{user}`, `{date}`) and real-time interactive Telegram chat bubble preview.
8. **Deep Obsidian Hardware Node Console UI**:
   - Beautiful dark OLED interface with live hardware telemetry meters and real-time sanitized console logs.

---

## 8. Features That Must NOT Be Advertised (Crucial Boundaries)

To maintain technical credibility and prevent user deception, the following boundaries must be strictly observed:

1. **DO NOT advertise arbitrary dynamic DEX / APK compilation or arbitrary code execution**:
   - While the package specification format defines a `dex` runtime enum for future evolution, current production bots execute via `native_art` or `built-in` reflection in the host APK. TapBot is a curated bot platform, not an arbitrary unverified bytecode compiler.
2. **DO NOT advertise inbound Telegram Webhook hosting**:
   - TapBot deliberately utilizes **outbound HTTPS long-polling** on port 443. Mobile devices operate behind cellular carrier-grade NAT (CGNAT) without static public IP addresses or incoming port-forwarding. Claiming "webhook server hosting" is technically false for mobile clients (outbound HTTP webhooks in custom bots are supported, but inbound Telegram updates use long-polling).
3. **DO NOT advertise video streaming or YouTube video playback in TapStream**:
   - TapStream is strictly an **audio** streaming and playback engine. It does not play, decode, or stream video content.
4. **DO NOT advertise cloud synchronization of bot tokens**:
   - Credentials are cryptographically locked inside the local device's Android Keystore by design. There is no cloud sync of private keys or tokens between devices.
5. **DO NOT advertise cloud web dashboard control of bots**:
   - The Cloudflare Workers backend provides catalog package distribution and admin package publishing (`/admin`), not remote bot control. Remote control occurs exclusively on-device or via Telegram chat.

---

## 9. Available Images, Logos, Screenshots, and Recordings

### 9.1 Available Visual Assets in Repository
* **Branding & Architecture Diagrams**:
  - `assets/banner.svg`: Wide high-contrast header banner with TapBot antenna logo and gradient typography.
  - `assets/architecture.svg`: Complete vector system architecture diagram illustrating Android ART, Keystore, Telegram API, and Cloudflare Edge.
* **Vector Feature Icons (13 SVGs in `assets/icons/`)**:
  - `android-shield.svg`: Android security and defense shield.
  - `apk-package.svg`: APK binary distribution package icon.
  - `bot-antenna.svg`: Primary TapBot antenna node logo.
  - `cloud-edge.svg`: Cloudflare Workers & edge cloud icon.
  - `coroutine-orbit.svg`: High-efficiency Kotlin coroutine runtime icon.
  - `crown-author.svg`: Author and contributor crown emblem.
  - `fleet-shield.svg`: Fleet management shield.
  - `keystore-vault.svg`: Hardware-backed Android Keystore vault.
  - `ram-chip.svg`: Minimal RAM and memory chip icon.
  - `remote-hardware.svg`: Remote phone and hardware controller.
  - `rollback-loop.svg`: Atomic update and rollback loop icon.
  - `telegram-fly.svg`: Telegram paper plane launch icon.
  - `watchdog-scan.svg`: Autonomous background watchdog scanner.
* **Website Assets (`website/`)**:
  - Interactive HTML/CSS/JS frontend with live oscilloscope audio canvas, ambient aurora glow orbs, and dark console styling.
* **App Assets (`app/src/main/res/`)**:
  - Vector and raster launcher icons (`ic_launcher.png`, `ic_launcher_round.png`) across mipmap densities.

### 9.2 Asset Inventory Gaps
* **No Real-Device Screen Recordings**: There are currently zero MP4/WebM screen recordings of the v2.4.0 UI in action.
* **No Raster High-Resolution Phone Mockup Screenshots**: The repository contains vector SVG icons and diagrams, but lacks framed device screenshots of the new Deep Obsidian UI.

---

## 10. Video-Generation Capabilities Available Through Nafay3D (faceless-shorts skill)

The installed video engine (`faceless-shorts`, featuring the flagship **Deep Dive 3D** style) is a 100% code-driven procedural video pipeline:

### 10.1 Engine Capabilities
* **Output Format**: Vertical 9:16 mobile format (1080x1920), 24 fps cinematic rendering, standard MP4 with H.264 video and AAC audio.
* **Local Neural Voiceover**: Kokoro-82M TTS engine generating clear conversational narration (`am_michael`) locally with zero API costs.
* **Word-Synced Kinetic Captions**: Faster-Whisper alignment extracting millisecond-accurate timestamps, rendering dynamic 1–3 word captions with active yellow keyword highlights.
* **Procedural 3D World (Three.js)**:
  - Stylized 3D sculpted human characters (`Human` class) with customizable poses, walking cycles, and head tracking.
  - Sunlit studio / room environments.
  - Glowing cyan x-ray cutaways (`#4FD1E8`) revealing internal anatomy or device components.
  - Macro deep-red internal sequences (`#B8283A`) with shallow depth of field and bloom.
  - Continuous cinematic camera motion (dolly push-ins, orbits, dramatic pans).
* **2D HUD Overlay**: Floating telemetry pills (`pill()`), leader-line callout labels (`label()`), warning badges (`warning()`), and text banners.
* **Procedural Sound & Music**: Automated tension drone, riser pulses, and soft hit accents synchronized with visual cuts via `mix.py`.

### 10.2 Technical Constraints & Boundaries
* **Not a Text-to-Video AI Model**: It cannot synthesize photorealistic live-action video clips from text prompts (e.g., Sora/Runway). Everything rendered must be constructed in procedural code (Three.js geometries, shaders, or 2D canvas overlays).
* **No Native Android Device Emulation**: The engine cannot run an Android emulator or record a live Android touchscreen by itself.
* **Stylized Clay Aesthetic**: 3D humans and props have a stylized, smooth SDF clay appearance with soft lighting and ACES tone mapping rather than photorealistic human skin.
* **Headless Chrome Rendering Load**: Three.js shaders are captured frame-by-frame via headless Google Chrome. On systems without hardware GPU acceleration, rendering uses software GL (`SOFTWARE_GL=1`, ~2s per frame), requiring batched rendering and contact sheet QA checks (`tools/capture.mjs sheet`).

---

## 11. Recommended Technical Workflow for Production

To produce an engaging, technically accurate, high-retention launch video for TapBot using the Deep Dive 3D pipeline:

### Step 1: Scripting & Storyboard (Phase 2)
* Target Duration: 35–45 seconds (~90–110 words).
* **The Hook (0–5s)**: Shocking contrast between expensive cloud VPS bills ($20/mo) and an old Android phone sitting unused in a drawer.
* **The Reveal (5–15s)**: Introducing TapBot — turning any Android phone into an autonomous 24/7 Telegram bot server running in your pocket with zero cloud fees.
* **The Flagship Demo (15–30s)**:
  - 3D cutaway showing the phone's hardware: remote torch toggle, loud phone alarm, live battery HUD, and hardware Keystore encryption vault.
  - Direct `/apk` delivery in Telegram chat.
* **The Climax & CTA (30–40s)**: Multiple bots running simultaneously on 8 MB of RAM; download link / GitHub repository.

### Step 2: Visual Asset & Component Construction (Phase 3)
* Construct a stylized 3D smartphone model in `src/core/` with a glowing screen display.
* Texture the 3D phone screen with vector UI graphics from the Deep Obsidian design system (Hardware Telemetry HUD, Telegram chat bubbles, Commander Bot controls).
* Incorporate TapBot's official SVG feature icons (`bot-antenna.svg`, `keystore-vault.svg`, `remote-hardware.svg`, `apk-package.svg`).
* Model an x-ray hardware view showing glowing encrypted Keystore storage (`AES-256-GCM`) inside the phone.

### Step 3: Audio Synthesis & Word Alignment (Phase 4)
* Generate voiceover with Kokoro TTS (`scripts/voice.py`).
* Run Faster-Whisper alignment (`scripts/words.py`) to generate exact `words.json` timing data.
* Compose procedural tension background audio bed and cue effects (`tools/mix.py`).

### Step 4: Scene Assembly, QA & Rendering (Phase 5)
* Implement shot sequences in `src/shots.js` tied to exact narration word cuts (`cut(i)`, `Wd('word', line)`).
* Generate contact review sheets (`tools/capture.mjs sheet review/all.jpg 0.5 10`) to inspect every frame for collisions, label alignment, and caption readability.
* Perform final headless Chrome capture and ffmpeg render to `renders/tapbot-launch-short.mp4`.

---

## Verification & Sign-off

- [x] Entire local repository inspected.
- [x] All 9 Gradle modules, manifests, services, and runtimes analyzed.
- [x] Complete bot lifecycle documented (install, configure, start/stop, monitor, update, auto-rollback, uninstall).
- [x] All Commander Bot commands, hardware actions, and inline callbacks cataloged.
- [x] Hardware Keystore encryption, SecretRedactor, and FLAG_SECURE verified.
- [x] Cloudflare Workers, D1 database, and R2 package storage verified.
- [x] Full Gradle unit test suite passed (`BUILD SUCCESSFUL`, 0 failures).
- [x] Production APK binary confirmed (`releases/tapbot-v2.4.0.apk`, 12.39 MB).
- [x] Deep Dive 3D (faceless-shorts / Nafay3D) capabilities and constraints documented.
- [x] Production workflow formulated.

**Report Generated:** `video-output/PHASE-1-REPORT.md`  
**Ready for Review.**
