# TapBot Android Compiled Binaries (v2.3.0)

Pre-built APK binaries for **TapBot: Native On-Device Telegram Bot Runner & Decentralized Bot Store**.

## Available Packages

| Package | Version | Build Type | Size | SHA-256 Checksum | Description |
|---|---|---|---|---|---|
| [`tapbot-v2.3.0.apk`](tapbot-v2.3.0.apk) | v2.3.0 (`230`) | Production Signed | 12.22 MB (12,817,107 bytes) | `55B367181A706F483E14F1148E4C4867D427AFFB2628CD0F030DF2E592F29E91` | **Recommended**. Latest v2.3.0 release APK signed with TapBot production keystore (v2 scheme verified). Includes live Gemini AI, RSS/Atom broadcaster, Inline Keyboards, Exon 2.0, Commander Hardware 2.0, TapStream Audio 2.0, Interactive Settings, Custom Bot Builder, and all bundled `.botpkg` archives. |
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
adb install -r tapbot-v2.3.0.apk
```
