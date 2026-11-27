# TapBot Android Compiled Binaries (v2.7.0)

Pre-built APK binaries for **TapBot: Native On-Device Telegram Bot Runner & Decentralized Bot Store**.

## Available Packages

| Package | Version | Build Type | Size | SHA-256 Checksum | Description |
|---|---|---|---|---|---|
| [`tapbot-v2.7.0.apk`](tapbot-v2.7.0.apk) | v2.7.0 (`270`) | Production Signed | 8.18 MB (8,576,690 bytes) | `6A5F5365A6747FAECE2CB74C4E48A379CA0B03DBA57F0BF4DE320C986A8645A9` | **Recommended**. Fleet Telemetry, Encrypted Backup & Home Screen Widget Release. Features 24h message throughput telemetry HUD & per-bot activity chips, encrypted `.tapbotbackup` export/import using PBKDF2 + AES-256-GCM, Android Home Screen AppWidget with 1-tap Start/Stop controls, 20 bundled flagship bots, and autonomous OTA updates. |
| [`tapbot-v2.6.0.apk`](tapbot-v2.6.0.apk) | v2.6.0 (`260`) | Production Signed | 8.16 MB (8,556,254 bytes) | `BF4EB083E7294CED9E861234337E42A5A9506DE420F54BDE6FBCBD4689B8F8BA` | Autonomous In-App Force Update & OTA Version Control Release. Features decentralized GitHub/CDN version manifest checks, non-dismissible mandatory update dialogs, direct APK download/installer launch, in-app "Check for Updates" trigger in Node Settings, bundled offline fleet of 20 flagship bots, In-App Bot Simulator, and My Fleet 2.0 batch controls. |
| [`tapbot-v2.5.0.apk`](tapbot-v2.5.0.apk) | v2.5.0 (`250`) | Production Signed | 8.13 MB (8,523,490 bytes) | `4895583BC183E6BD23C25242A88544A40250CF8D52F3674BAE499133BBF2D8FC` | Previous Fleet 2.0 & Interactive Simulator Release. Features 1-Tap Fleet Batch controls (Start All Idle, Stop All Active), real-time search & status filtering (`All`, `Running`, `Stopped`), In-App Interactive Bot Simulator sandbox modal, Telegram `setMyCommands` automated command menu publishing, automatic clipboard Telegram bot token detection, and all 20 bundled flagship bot packages. |
| [`tapbot-v2.4.0.apk`](tapbot-v2.4.0.apk) | v2.4.0 (`240`) | Production Signed | 8.03 MB (8,424,401 bytes) | `A65DC2B48E75CABA36038B799FBEC1BB384BEDEAB38757A6764C2FBEB1BA0863` | Play Store for Bots UI/UX redesign. Authentic consumer-grade bot marketplace with Spotlight Carousel, Top Charts rankings, Categories browser, 72dp squircle app identities, 4-metric pill matrix, dynamic green Install -> Start/Stop CTA, Telegram chat previews, Google Play Data Safety disclosure, and D1 SQLite storage fallback. |
| [`tapbot-v2.3.0.apk`](tapbot-v2.3.0.apk) | v2.3.0 (`230`) | Production Signed | 12.22 MB (12,817,107 bytes) | `55B367181A706F483E14F1148E4C4867D427AFFB2628CD0F030DF2E592F29E91` | Previous v2.3.0 release APK signed with TapBot production keystore (v2 scheme verified). Includes live Gemini AI, RSS/Atom broadcaster, Inline Keyboards, Exon 2.0, Commander Hardware 2.0, TapStream Audio 2.0, Interactive Settings, Custom Bot Builder, and all bundled `.botpkg` archives. |
| [`TapBot-v1.0.0-release.apk`](TapBot-v1.0.0-release.apk) | v1.0.0 | Production Signed | 12.5 MB | `36099EE494DF83F90EFB3BC2276EE4E423ED02082AD3CDB96C13908C182A2E35` | Legacy v1.0.0 release APK signed with TapBot production keystore. |
| [`TapBot-v1.0.0-debug.apk`](TapBot-v1.0.0-debug.apk) | v1.0.0 | Debug | 19.1 MB | `C7FA3C23551F9A14681E954839083BD9BBD592CE47CED838BBF94D84E04B528A` | Legacy v1.0.0 development build with debug logging. |

## Verification (`apksigner`)
- Certificate DN: `CN=TapBot, OU=Mobile, O=VarshuAi, L=Global, ST=Earth, C=US`
- Certificate SHA-256: `4a62d1b5fee463cc601dc3d8797961dcf9612acac09c0ccc99cec78668d388bc`

## Official GitHub Releases
Direct downloads and release notes:
**[GitHub Releases — VarshuDev/tapbot](https://github.com/VarshuDev/tapbot/releases)**

## Installation
```bash
adb install -r tapbot-v2.7.0.apk
```
