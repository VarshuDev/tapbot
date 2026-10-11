# TapBot - Terms of Service

**Effective Date:** October 11, 2026  
**Last Updated:** October 11, 2026  

Welcome to **TapBot** ("Application", "Service", "we", "us", or "our"). By downloading, installing, accessing, or using TapBot, you agree to be bound by these Terms of Service ("Terms"). If you do not agree to these Terms, do not install or use the Application.

---

## 1. Description of the Application

TapBot is an open-source, on-device automation tool for Android that enables users to manage, configure, and execute custom Telegram bots natively on their devices using the official Telegram Bot API. TapBot operates entirely on-device, running long-polling connections directly between your Android device and Telegram's official API servers.

---

## 2. User Responsibilities & Conduct

### 2.1 Telegram Bot API Compliance
You are solely responsible for obtaining your Telegram Bot API tokens from Telegram's official `@BotFather` bot. When using TapBot, you must comply with:
- The [Telegram Terms of Service](https://telegram.org/tos)
- The [Telegram Bot API Terms of Service](https://core.telegram.org/bots/api)

### 2.2 Prohibited Activities
You agree **NOT** to use TapBot to run bots that:
- Send unsolicited spam, bulk advertising, or malicious messages.
- Harass, abuse, stalk, or infringe upon the privacy rights of others.
- Engage in illegal, fraudulent, or deceptive conduct.
- Distribute malware, spyware, phishing payloads, or illegal files.
- Overwhelm, denial-of-service, or deliberately stress external servers.

---

## 3. On-Device Execution & Third-Party Dependencies

### 3.1 Local Key Storage
All credentials, API tokens, and secret configuration values are encrypted and stored solely on your device using Android hardware Keystore and encrypted preferences. TapBot developers never have access to your tokens, messages, or keys.

### 3.2 Battery & Background Processing Limitations
TapBot utilizes standard Android Foreground Services with persistent ongoing notifications to maintain long-polling connections. You acknowledge that:
- Background execution depends on Android OS battery management policies and device manufacturer customizations (e.g., Samsung One UI, Xiaomi HyperOS/MIUI, OnePlus OxygenOS, Huawei EMUI).
- Aggressive OEM battery savers or low-memory conditions may terminate or suspend foreground tasks.
- You may need to manually exempt TapBot from battery optimization in your device's system settings to maintain uninterrupted 24/7 uptime.

---

## 4. Intellectual Property & License

TapBot source code is released under the **Apache License 2.0** or **MIT License** (as designated in the source repository). You are free to inspect, modify, and build the software according to the applicable open-source license.

Telegram is a registered trademark of Telegram FZ-LLC. TapBot is an independent application and is not affiliated with, endorsed by, or sponsored by Telegram.

---

## 5. Disclaimer of Warranties

THE APPLICATION IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.

WE DO NOT WARRANT THAT:
1. THE SERVICE WILL BE UNINTERRUPTED, TIMELY, SECURE, OR ERROR-FREE;
2. BOT CONNECTIONS WILL MAINTAIN 100% PERSISTENCE ACROSS ALL ANDROID OEM BATTERY POLICIES OR CELLULAR RECONNECTIONS;
3. TELEGRAM SERVERS OR NETWORK CONNECTIONS WILL ALWAYS BE ACCESSIBLE.

---

## 6. Limitation of Liability

TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE DEVELOPERS, CONTRIBUTORS, OR AFFILIATES BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, OR ANY LOSS OF PROFITS, DATA, USE, GOODWILL, OR OTHER INTANGIBLE LOSSES, RESULTING FROM:
- YOUR ACCESS TO OR USE OF OR INABILITY TO ACCESS OR USE THE APPLICATION;
- ANY TELEGRAM ACCOUNT RESTRICTIONS OR BANS IMPOSED DUE TO BOT ACTIVITIES CONFIGURED BY THE USER;
- DEVICE BATTERY CONSUMPTION, NETWORK DATA USAGE, OR MEMORY CONSUMPTION.

---

## 7. Changes to Terms

We reserve the right to modify or replace these Terms at any time. When updates are published, the "Last Updated" date at the top of this document will be updated. Continued use of the Application after changes are posted constitutes acceptance of the modified Terms.

---

## 8. Support & Contact Information

For support questions, bug reports, feature requests, or legal inquiries, please contact:

- **Email:** `support@tapbot.app`
- **GitHub Repository & Issue Tracker:** [https://github.com/VarshuAi/tapbot](https://github.com/VarshuAi/tapbot)
- **Developer Profile:** [VarshuAi](https://github.com/VarshuAi)
