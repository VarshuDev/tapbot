# Privacy Policy for TapBot

**Effective Date:** October 10, 2026  
**Developer:** VarshuDev  
**App:** TapBot (com.tapbot)  

## 1. Overview
TapBot is an open-source, on-device automation and Telegram bot execution runner designed for Android. TapBot allows users to deploy and run lightweight automation bots locally on their Android devices without relying on third-party cloud hosting servers.

Your privacy and security are fundamental principles of TapBot. This Privacy Policy outlines how your information is handled.

---

## 2. Information We Collect
**TapBot collects zero personal information, usage analytics, device tracking identifiers, or advertising IDs.**

- **No Analytics / Telemetry**: TapBot does not include any third-party analytics SDKs (e.g., Firebase Analytics, Google Analytics, Mixpanel, etc.).
- **No Advertising**: TapBot does not display advertisements or integrate ad networks.
- **No Account Registration**: You do not need an account to use TapBot.

---

## 3. How User Data & Bot Credentials Are Handled
When you use TapBot to run Telegram automation bots, you may input configuration secrets such as:
- Telegram Bot Tokens
- API IDs / Hashes
- Cloudflare Catalog URLs

### Secure Storage:
- All sensitive credentials entered by the user are stored **exclusively on the local device**.
- Credentials are encrypted using the **Android Keystore System** with hardware-backed encryption keys (`AndroidKeyStore`) using AES-256-GCM.
- Secrets are **never** uploaded to our servers, logged to disk in plaintext, or transmitted to any third party.

---

## 4. Network Communications
TapBot makes outbound network connections solely to:
1. **Official Telegram APIs (`api.telegram.org`)**:
   - To send messages, receive updates via HTTP long-polling, and execute bot actions as initiated by the user.
2. **Cloudflare Bot Catalog (`tapbot-catalog.workers.dev`)**:
   - To fetch the list of available verified bot packages and release notes.

All external communications are conducted over encrypted HTTPS connections (TLS 1.2+).

---

## 5. Device Permissions Used
- **`INTERNET` & `ACCESS_NETWORK_STATE`**: Required to communicate with Telegram and download bot packages.
- **`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`**: Required to keep the bot polling process active while the screen is off, displaying an ongoing persistent status notification to ensure transparency.
- **`POST_NOTIFICATIONS`**: Required to display runner controls and notifications on Android 13+.
- **`WAKE_LOCK`**: Required to prevent CPU deep sleep while executing polling cycles.

---

## 6. Children's Privacy
TapBot does not knowingly collect or solicit any personal information from children under the age of 13.

---

## 7. Open Source & Transparency
TapBot is open-source. You can inspect the source code, build process, and network requests at:  
[https://github.com/VarshuDev/tapbot](https://github.com/VarshuDev/tapbot)

---

## 8. Contact
If you have questions regarding this Privacy Policy, you can reach out via GitHub:  
[https://github.com/VarshuDev/tapbot/issues](https://github.com/VarshuDev/tapbot/issues)
