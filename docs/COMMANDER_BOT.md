# 🚀 TapBot Commander — Flagship On-Device Telegram Bot

> **The premier flagship Telegram Bot designed specifically for the TapBot Android APK ecosystem.**
> Runs 100% locally on Android ART without cloud servers or VPS hosting. Provides remote device telemetry, TapBot bot fleet management, remote Android hardware controls, direct APK delivery in Telegram, and autonomous background watchdog alerts.

---

## ⚡ Overview & Vision

Traditional Telegram bots run on remote servers (AWS, DigitalOcean, VPS) or webhooks. Because they exist in the cloud, they have zero awareness of your physical mobile devices.

**TapBot Commander** is the flagship bot of the TapBot platform. By executing inside TapBot's native Android ART runtime on your smartphone, it bridges the gap between Telegram and native Android hardware:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          TELEGRAM CLIENT                               │
│  User sends: /status, /battery, /bots, /startbot, /ring, /apk, /logs   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS Long-Polling (Port 443)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     ANDROID DEVICE (TapBot Host)                       │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │            CommanderBotRuntime (In-Process Coroutines)           │  │
│  │                                                                  │  │
│  │   ┌──────────────────────┐        ┌───────────────────────────┐  │  │
│  │   │   Device Telemetry   │        │     Bot Fleet Manager     │  │  │
│  │   │   • Battery % & Temp │        │     • List Active Bots    │  │  │
│  │   │   • Physical RAM     │        │     • Start / Stop / Rest │  │  │
│  │   │   • Internal Storage │        │     • Live Logs & Export  │  │  │
│  │   │   • WiFi / LTE Stats │        └─────────────┬─────────────┘  │  │
│  │   └──────────┬───────────┘                      │                │  │
│  │              │                                  │                │  │
│  │              ▼                                  ▼                │  │
│  │   ┌──────────────────────┐        ┌───────────────────────────┐  │  │
│  │   │ Remote Phone Actions │        │    Autonomous Watchdog    │  │  │
│  │   │ • Ring Phone Alarm   │        │    • Low Battery (<15%)   │  │  │
│  │   │ • Flashlight Toggle  │        │    • Bot Crash Alerts     │  │  │
│  │   │ • Push Notifications │        └───────────────────────────┘  │  │
│  │   │ • Clipboard Read/Set │                                       │  │
│  │   │ • Haptic Vibration   │                                       │  │
│  │   └──────────┬───────────┘                                       │  │
│  │              │                                                   │  │
│  │              ▼                                                   │  │
│  │   ┌──────────────────────────────────────────────────────────┐   │  │
│  │   │ Native APK Document Delivery                             │   │  │
│  │   │ • Sends app-debug.apk directly via Telegram sendDocument │   │  │
│  │   │ • Computes on-device SHA-256 integrity hash              │   │  │
│  │   └──────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🌟 Command Reference

### 📱 1. Device Telemetry
| Command | Description |
| :--- | :--- |
| `/status`, `/sys` | Complete system HUD (Battery level & temp, RAM, Disk, Network, Fleet summary, Uptime). |
| `/battery` | Detailed battery diagnostics: percentage, charging status (AC/USB/Wireless), voltage (mV), health, Doze whitelist status. |
| `/ram`, `/mem` | Total physical RAM, available memory, used memory percentage, and TapBot ART JVM heap metrics. |
| `/storage`, `/disk` | Internal storage diagnostics with visual usage progress bar (`[████████░░] 78%`). |
| `/network`, `/net` | Active connection transport (Wi-Fi SSID, Cellular LTE/5G, Ethernet) and port status. |
| `/uptime` | System uptime since Android boot and TapBot service runtime. |

### 🤖 2. Bot Fleet & Live Log Management
| Command | Description |
| :--- | :--- |
| `/bots`, `/fleet` | Lists all installed bots on the device with live statuses (`🟢 RUNNING`, `⚪ STOPPED`, `🔴 CRASHED`). |
| `/startbot <id>` | Starts an installed bot instance remotely from Telegram. |
| `/stopbot <id>` | Gracefully stops a running bot instance. |
| `/restartbot <id>` | Restarts a bot instance with a clean coroutine lifecycle. |
| `/logs <id> [lines]` | Streams the latest N lines of live sanitized logs directly into chat. |
| `/exportlogs <id>` | Generates and sends a `.txt` log file document directly via Telegram. |

### 🎮 3. Remote Android Hardware Controls
| Command | Description |
| :--- | :--- |
| `/ring [seconds]` | Plays an audible alert ringtone on the phone at full volume for 15s to locate a misplaced device. |
| `/stopring` | Immediately silences the ringing alarm. |
| `/torch [on\|off]` | Toggles the host phone's camera flashlight / torch remotely. |
| `/notify <message>` | Posts a high-priority heads-up notification on the Android device's notification shade. |
| `/clipboard` | Reads the current text content from the Android system clipboard. |
| `/setclip <text>` | Copies text to the Android device clipboard remotely. |
| `/vibrate` | Triggers a 500ms haptic vibration on the phone. |

### 📦 4. APK Delivery & Verification
| Command | Description |
| :--- | :--- |
| `/apk`, `/download` | Reads the installed TapBot APK on-device, calculates SHA-256, and sends the `.apk` file directly to your chat. |
| `/version`, `/release` | Displays app version name, version code, target SDK (API 35), minimum SDK (API 29), and build metadata. |

### 🛠 5. Onboarding & Guides
| Command | Description |
| :--- | :--- |
| `/setup` | Complete step-by-step walkthrough for generating tokens with `@BotFather` and finding your User ID. |
| `/batteryguide` | OEM-specific guide to disable battery killers (Samsung Never Sleeping Apps, Xiaomi Autostart, Pixel Unrestricted). |
| `/diagnose` | Automated health check testing Telegram Bot API roundtrip latency, Keystore readiness, and background status. |

### 🛍 6. Cloudflare Bot Store
| Command | Description |
| :--- | :--- |
| `/store`, `/catalog` | Lists featured bots available in the decentralized TapBot store. |
| `/search <query>` | Searches the catalog by name, category, or description keywords. |

---

## 🚨 Autonomous Watchdog System

TapBot Commander runs a continuous background watchdog coroutine that monitors host health:

1. **Low Battery Alerts**:
   - If the device battery drops below the threshold (default: 15%) and the phone is discharging (unplugged), Commander pushes an urgent alert to the owner's Telegram chat:
     > `⚠️ LOW BATTERY WARNING!`  
     > `The host phone running your TapBot fleet is at 14% and discharging!`  
     > `Please connect a charger to prevent Android from shutting down your bots.`
2. **Bot Crash Alerts**:
   - If any installed bot encounters an uncaught exception or runtime failure, Commander immediately alerts the owner with the crash error stack, recommending `/logs` and `/restartbot`.

---

## 🔒 Security & Owner Access Control

- **Owner-Only Commands**: Sensitive commands (`/status`, `/ring`, `/torch`, `/notify`, `/clipboard`, `/setclip`, `/stopbot`, `/startbot`, `/restartbot`, `/logs`, `/exportlogs`) are strictly gated by the owner's numeric Telegram User ID (`owner_id`).
- **Public Commands**: Anyone can run `/start`, `/help`, `/about`, `/apk`, `/setup`, and `/store`.
- **Zero-Leak Credentials**: Bot tokens and owner IDs are encrypted with hardware-backed Android Keystore keys (`AES-256-GCM`).
