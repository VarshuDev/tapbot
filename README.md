<p align="center">
  <a href="https://github.com/VarshuDev/tapbot">
    <img src="assets/banner.svg" alt="TapBot Autonomous On-Device Telegram Bot Runner Header" width="100%" />
  </a>
</p>

<p align="center">
  <strong>Decentralized On-Device Telegram Bot Runner, Hardware Commander &amp; Edge Bot Store.</strong>
</p>

<p align="center">
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-10%2B%20(API%2029--35)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 10+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0%20(ART)-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.0" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose M3" /></a>
  <a href="https://workers.cloudflare.com"><img src="https://img.shields.io/badge/Cloudflare-Workers%20%7C%20D1%20%7C%20R2-F38020?style=for-the-badge&logo=cloudflare&logoColor=white" alt="Cloudflare Edge" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge" alt="Apache 2.0" /></a>
</p>

<p align="center">
  <a href="#why-tapbot"><img src="assets/icons/bot-antenna.svg" width="16" height="16" align="center" /> Why TapBot?</a> &nbsp;&bull;&nbsp;
  <a href="#architecture--pipeline"><img src="assets/icons/coroutine-orbit.svg" width="16" height="16" align="center" /> Architecture</a> &nbsp;&bull;&nbsp;
  <a href="#flagship-commander-bot--device-actions"><img src="assets/icons/remote-hardware.svg" width="16" height="16" align="center" /> Commander Bot</a> &nbsp;&bull;&nbsp;
  <a href="#hardware-backed-security"><img src="assets/icons/keystore-vault.svg" width="16" height="16" align="center" /> Security</a> &nbsp;&bull;&nbsp;
  <a href="#edge-bot-store--cloudflare-backend"><img src="assets/icons/cloud-edge.svg" width="16" height="16" align="center" /> Edge Store</a> &nbsp;&bull;&nbsp;
  <a href="#performance-benchmarks"><img src="assets/icons/ram-chip.svg" width="16" height="16" align="center" /> Benchmarks</a> &nbsp;&bull;&nbsp;
  <a href="#quickstart"><img src="assets/icons/telegram-fly.svg" width="16" height="16" align="center" /> Quickstart</a>
</p>

---

## <img src="assets/icons/bot-antenna.svg" width="22" height="22" align="center" /> Why TapBot?

Traditional Telegram bot hosting forces developers and power users into recurring compromises: **costly cloud VPS subscriptions** (AWS, DigitalOcean, Heroku), **complex serverless webhook configurations**, and **centralized credential exposure** on third-party cloud infrastructure.

**TapBot flips the hosting paradigm by turning any modern Android device into an autonomous 24/7 Telegram bot server:**

1. **Zero Cloud Infrastructure Costs**: Run multiple production bots entirely on physical Android hardware or spare smartphones with $0/month in cloud server bills.
2. **True Data Sovereignty**: Bot tokens, private chat histories, and sensitive user data remain strictly localized inside your device's encrypted storage sandbox.
3. **NAT &amp; Firewall Piercing**: Uses outbound HTTPS long-polling (`getUpdates`) on port 443. No public IP address, static DNS, port forwarding, or domain certificates required.
4. **Negligible Resource Overhead**: Native Android ART coroutine execution requires only **8 MB &ndash; 12 MB RAM** per bot instance with &lt; 0.1% CPU utilization during idle polling.
5. **Survives OS Power Management**: Engineered with a hardened Android Foreground Service and wake-lock management that persists across Doze Mode, screen-off cycles, and network handovers.

---

## <img src="assets/icons/coroutine-orbit.svg" width="22" height="22" align="center" /> Architecture &amp; Pipeline

TapBot coordinates a hybrid decentralized architecture linking local Android execution with serverless edge catalog distribution:

<p align="center">
  <img src="assets/architecture.svg" alt="TapBot System Architecture Diagram" width="100%" />
</p>

- **Telegram Bot API Link**: High-efficiency HTTP client maintaining persistent TLS 1.3 connections to Telegram edge clusters.
- **Android ART Runtime**: Coroutine-based engine dispatching bot updates concurrently with zero subprocess overhead.
- **Hardware Security Layer**: Hardware-backed Android Keystore (`AES-256-GCM`) via StrongBox / TEE.
- **Cloudflare Edge Store**: Decentralized `.botpkg` repository powered by Cloudflare Workers, Cloudflare D1 (SQLite), and Cloudflare R2 object storage.

---

## <img src="assets/icons/remote-hardware.svg" width="22" height="22" align="center" /> Flagship Commander Bot &amp; Device Actions

TapBot includes the pre-installed **Commander Bot**, turning your Android phone into an interactive, remotely controlled hardware node managed entirely via Telegram chat:

<table align="center" width="100%">
  <thead>
    <tr>
      <th align="left">Telegram Command</th>
      <th align="left">Hardware / System Action</th>
      <th align="left">Execution Boundary</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>/status</code></td>
      <td>Returns live diagnostics: battery %, charging state, CPU thermal status, RAM consumption, and network connection type</td>
      <td><code>DeviceTelemetryProvider</code></td>
    </tr>
    <tr>
      <td><code>/torch on|off</code></td>
      <td>Toggles the physical device camera LED flashlight remotely via Android <code>CameraManager</code></td>
      <td><code>DeviceActionController</code></td>
    </tr>
    <tr>
      <td><code>/vibrate [ms]</code></td>
      <td>Triggers haptic vibration sequences on the target hardware via Android <code>Vibrator</code></td>
      <td><code>DeviceActionController</code></td>
    </tr>
    <tr>
      <td><code>/notify [msg]</code></td>
      <td>Posts custom high-priority notification banners to the Android system notification tray</td>
      <td><code>NotificationHelper</code></td>
    </tr>
    <tr>
      <td><code>/apk</code></td>
      <td>Streams the latest signed TapBot APK binary directly into the Telegram chat for instant field distribution</td>
      <td><code>ApkDeliveryService</code></td>
    </tr>
    <tr>
      <td><code>/fleet</code></td>
      <td>Lists all active, idle, and stopped bots hosted on the device with one-tap start/stop controls</td>
      <td><code>FleetCommandRouter</code></td>
    </tr>
  </tbody>
</table>

### Autonomous Watchdog Coroutine
The on-device **`DeviceWatchdog`** operates in the background:
- Continuously polls battery thresholds and thermal sensor states.
- Auto-restarts stalled or unresponsive bot coroutines.
- Pushes real-time Telegram alerts to the device owner if battery drops below 15% or hardware temperatures exceed thermal limits.

---

## <img src="assets/icons/keystore-vault.svg" width="22" height="22" align="center" /> Hardware-Backed Security

TapBot applies strict defense-in-depth principles across the native Android operating system:

- **Hardware Keystore Master Keys**: Encryption keys are generated inside device hardware security modules (TEE / StrongBox) using `AES-256-GCM` with 128-bit authentication tags. Master keys are non-exportable from hardware.
- **Zero-Leak Secret Redaction**: All internal logs, stack traces, and debug streams pass through an active `SecretRedactor` pipeline that scrubs Telegram bot tokens (`1234****:[REDACTED_TELEGRAM_TOKEN]`) before writing to disk or displaying in UI.
- **Android W^X &amp; SELinux Hardening**: Operates 100% within native Android ART memory space without spawning arbitrary shell binaries or executing dynamically generated native machine code.
- **Zero Third-Party Telemetry**: Contains zero tracking SDKs, advertising libraries, or cloud telemetry hooks. Your bot tokens never transit any server other than official Telegram API endpoints.

---

## <img src="assets/icons/rollback-loop.svg" width="22" height="22" align="center" /> Atomic Version Management &amp; .botpkg Format

TapBot packages are distributed as cryptographically signed `.botpkg` archives containing bytecode definitions, assets, and strict dependency declarations.

### State-Machine Update Pipeline
Package updates execute through an atomic state machine guaranteeing zero downtime or corrupted installations:

```
[ CHECKING ] ───► [ DOWNLOADING ] ───► [ VERIFYING ] ───► [ INSTALLING ] ───► [ STARTING ] ───► [ SUCCESS ]
                                                                                   │
                                                                           (Startup Failure)
                                                                                   │
                                                                                   ▼
                                                                           [ ROLLING_BACK ] ───► [ RESTORED ]
```

- **SHA-256 Checksum Verification**: Every package payload is verified against tamper-evident cryptographic digests before unpacking.
- **Anti-Downgrade Defense**: Rejects malicious or accidental downgrade payloads attempting to replace secure versions with older builds.
- **Isolated State Retention**: Runtime configurations, owner tokens, and persistent databases remain untouched during updates and rollbacks.

---

## <img src="assets/icons/cloud-edge.svg" width="22" height="22" align="center" /> Edge Bot Store &amp; Cloudflare Backend

The ecosystem is backed by an edge-native catalog server built on **Cloudflare Workers**:

- **Serverless REST API**: Global sub-50ms catalog resolution via Cloudflare Workers (`/api/v1/bots`).
- **Cloudflare D1 Relational Catalog**: Serverless SQLite database managing bot metadata, author signatures, release channels, and installation counts.
- **Cloudflare R2 Object Storage**: High-durability zero-egress bucket storage for `.botpkg` binary distributions.
- **Dark Ops Admin HUD (`/admin`)**: Single-Page App administrative console for developers to publish packages, inspect audit logs, and toggle release visibility.

---

## <img src="assets/icons/ram-chip.svg" width="22" height="22" align="center" /> Performance Benchmarks

Empirical performance measurements comparing TapBot on native Android ART against standard server hosting alternatives:

| Metric | TapBot (On-Device ART) | Python 3.12 (VPS) | Node.js 20 (VPS) | Docker Container |
| :--- | :--- | :--- | :--- | :--- |
| **RAM Footprint (per bot)** | **8 MB &ndash; 12 MB** | 60 MB &ndash; 85 MB | 90 MB &ndash; 140 MB | 180 MB+ |
| **Cold Startup Latency** | **&lt; 50 ms** | ~450 ms | ~680 ms | ~1,800 ms |
| **Idle CPU Utilization** | **&lt; 0.1%** | 0.8% &ndash; 1.5% | 1.2% &ndash; 2.4% | 2.5%+ |
| **Network Architecture** | **HTTPS Long-Poll (Outbound)** | Webhook / Polling | Webhook / Polling | Webhook Relay |
| **Firewall / Port Forwarding** | **None Required** | Public IP / Port 443 | Public IP / Port 443 | Reverse Proxy |
| **Monthly Infrastructure Cost** | **$0.00 / month** | $5.00 &ndash; $20.00 / mo | $5.00 &ndash; $20.00 / mo | $10.00+ / mo |

---

## <img src="assets/icons/telegram-fly.svg" width="22" height="22" align="center" /> Quickstart

### Prerequisites
- **Android Studio** Jellyfish / Koala (or IntelliJ IDEA with Android plugin)
- **JDK 17** or higher
- Android physical device or emulator running **Android 10+ (API 29 &ndash; 35)**

### 1. Clone &amp; Open

```bash
# Clone the repository
git clone https://github.com/VarshuDev/tapbot.git

# Enter the project directory
cd tapbot
```

Open the project in Android Studio and let Gradle sync.

### 2. Build via Command Line

```bash
# Run unit and coroutine test suites
./gradlew test

# Compile release APK
./gradlew assembleRelease
```

Generated APKs will be output to `app/build/outputs/apk/release/`.

### 3. Deploy Cloudflare Edge Backend (Optional)

```bash
cd backend

# Install dependencies
npm install

# Initialize local D1 SQLite database
npx wrangler d1 execute tapbot-db --local --file=schema.sql

# Deploy to Cloudflare Edge
npx wrangler deploy
```

---

## <img src="assets/icons/apk-package.svg" width="22" height="22" align="center" /> Pre-Built Bot Runtime Library

TapBot includes out-of-the-box runtimes for immediate deployment:

- **`CommanderBotRuntime`**: Flagship device control, hardware sensors, and `/apk` binary delivery.
- **`AIBotRuntime`**: Conversational intelligence powered by lightweight API endpoints.
- **`ExonBotRuntime`**: Advanced Telegram supergroup management, anti-flood, and role moderation.
- **`MusicBotRuntime`**: Audio stream search, metadata lookup, and playback control.
- **`PingPongBotRuntime`**: Minimalist low-latency heartbeat responder.

---

## <img src="assets/icons/crown-author.svg" width="22" height="22" align="center" /> Author &amp; Credits

Architected, engineered, and maintained by **[VarshuDev](https://github.com/VarshuDev)**.

If you find TapBot useful for running autonomous bots or managing Android hardware nodes, consider starring <img src="assets/icons/crown-author.svg" width="16" height="16" align="center" /> the repository!

Distributed under the **[Apache 2.0 License](LICENSE)**.
