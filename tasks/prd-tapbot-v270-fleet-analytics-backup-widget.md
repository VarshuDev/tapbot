# TapBot v2.7.0 PRD: Fleet Analytics Dashboard, Encrypted Backup & Restore, and Android Home Screen AppWidget

## Goal & Executive Summary
TapBot has evolved into a premier on-device Telegram bot node with 20 daily-usage flagship bots, an in-app interactive simulator, and autonomous force-update version control. **TapBot v2.7.0** transforms TapBot into an enterprise-grade autonomous bot station by introducing:
1. **Fleet Analytics & Live Performance Dashboard**: Real-time throughput metrics (messages processed, commands dispatched, error logs, active uptime) and 24-hour activity sparklines per bot.
2. **Encrypted Fleet Backup & Migration (`.tapbotbackup`)**: 1-tap authenticated AES-256-GCM export and restore of all configured bots, tokens, and custom workflow definitions.
3. **Android Home Screen AppWidget (Glance / RemoteViews)**: A glanceable Android home screen widget displaying live running bots, phone battery/thermal health, and 1-tap "Start All / Stop All" emergency controls.
4. **Production Release Packaging (v2.7.0 / Build 270)**: apksigner-verified signed binary bundling all features, with OTA version.json manifest updates.

---

## User Stories & Architecture

### Phase 1: Fleet Telemetry & Message Throughput Tracking
- **US-P1-01: Fleet Analytics Data Models & Atomic Rolling Metric Store**
  - Implement `BotMetricSnapshot` and `FleetAnalyticsSummary` in `core:model`.
  - Implement `RollingBotMetricStore` in `core:runner` for atomic counters and disk persistence.
- **US-P1-02: Telemetry Interceptor & BotInstanceManager Event Hooking**
  - Hook update dispatchers in `BotInstanceManager` and `BotRuntime` to record message receipts and command executions.
  - Expose reactive `StateFlow<FleetAnalyticsSummary>`.

### Phase 2: Encrypted Fleet Backup & Restore (.tapbotbackup)
- **US-P2-01: Encrypted Backup Archive Spec & FleetBackupManager**
  - Implement `FleetBackupArchive` in `core:model`.
  - Implement `FleetBackupManager` in `core:security` with PBKDF2 + AES-256-GCM authenticated encryption/decryption.
- **US-P2-02: Backup & Restore UI in SettingsScreen**
  - Add "Fleet Backup & Migration" section in `SettingsScreen.kt` with file picker and password dialogs.

### Phase 3: Fleet Analytics UI & Live Performance Dashboard
- **US-P3-01: Fleet Analytics HUD & Per-Bot Throughput Badges**
  - Add 24-hour message throughput cards and uptime gauges to `MyBotsScreen.kt`.
  - Display live message count and command rate badges on each `BotInstanceCard`.

### Phase 4: Android Home Screen AppWidget & Production Release
- **US-P4-01: TapBot Quick Fleet AppWidget**
  - Implement `TapBotFleetAppWidgetProvider` with live running count, battery indicator, and 1-tap pending intents for Start All / Stop All.
- **US-P4-02: Production Release Packaging v2.7.0 & Documentation**
  - Bump `versionCode = 270`, `versionName = "2.7.0"`.
  - Build signed `releases/tapbot-v2.7.0.apk` and update `version.json` and website mirrors.
