# Google Play Foreground Service Declaration Guide

For apps targeting Android 14+ (API 34/35), Google Play requires developers to submit a declaration form explaining why the app uses `FOREGROUND_SERVICE` and its specific type (`FOREGROUND_SERVICE_DATA_SYNC`).

Use the exact answers below when filling out the Play Console Policy Declaration form:

---

### 1. Foreground Service Type Selected
- **Type**: `DATA_SYNC` (`android:foregroundServiceType="dataSync"`)

---

### 2. User-Facing Purpose of the Foreground Service
**Prompt in Play Console:** *"Describe the core feature of your app that requires this foreground service and explain how the user is aware of the service running."*

**Recommended Submission Answer:**
```text
TapBot is an on-device automation and bot runner tool that executes lightweight developer bots (such as Telegram group management automations).

To ensure bots remain responsive to incoming chat messages and commands while the app is in the background or when the device screen is locked, TapBot initiates a continuous data synchronization foreground service that performs HTTP long-polling against the messaging provider's API.

The user explicitly starts and stops this service using an interactive 'START BOT' / 'STOP BOT' toggle inside the app. While running, the service displays a mandatory, persistent ongoing status notification with live state metrics and instant 'Open' and 'Stop' quick-action buttons so the user is always in full control of background execution.
```

---

### 3. Why Can't WorkManager or JobScheduler Be Used Instead?
**Prompt in Play Console:** *"Why can't this task be accomplished using WorkManager or JobScheduler?"*

**Recommended Submission Answer:**
```text
WorkManager and JobScheduler are designed for deferrable, opportunistic background work with minimum periodic intervals of 15 minutes. Interactive chat bots require real-time latency (<2 seconds) to respond to chat commands and messages from users. Using WorkManager would result in severe delays, broken chat interactions, and frequent process termination by Android battery optimization. A foreground service with an active notification is required to maintain real-time bidirectional synchronization as explicitly commanded by the user.
```

---

### 4. Video Link Demonstration Requirement
Google Play requires a short video demonstration (typically a 15–30 second YouTube unlisted video link) showing:
1. The user opening TapBot and selecting a bot.
2. The user tapping "START BOT".
3. The persistent foreground service notification appearing in the Android notification drawer.
4. The user locking the screen / minimizing the app, showing that the notification remains visible.
5. The user stopping the bot using the "STOP" button.
