# Google Play Data Safety Questionnaire Answers

Use these answers when completing the **Data Safety** form in Google Play Console:

---

### Data Collection & Sharing Overview
- **Does your app collect or share any of the required user data types?**  
  👉 **No.** (TapBot does not collect, track, or share any personal data with third parties or advertising brokers).

- **Is all of the user data collected by your app encrypted in transit?**  
  👉 **Yes.** (All network requests made by the app use TLS 1.2+ HTTPS).

- **Do you provide a way for users to request that their data be deleted?**  
  👉 **Yes.** (Users can tap 'Delete Credentials' at any time inside the app, which immediately purges keys from hardware Keystore).

---

### Specific Data Type Declarations

1. **User Credentials (API Keys, Bot Tokens)**:
   - *Collected?* **No** (Processed locally on device only).
   - *Shared?* **No**.
   - *Stored?* Stored locally in hardware-encrypted Android Keystore (`AndroidKeyStore`).

2. **Location**: **None collected**.
3. **Personal Info (Name, Email, Phone)**: **None collected**.
4. **Financial Info**: **None collected**.
5. **Health & Fitness**: **None collected**.
6. **Messages**:
   - TapBot does not collect or archive messages. Transient bot polling reads messages solely to execute user-defined automation triggers and does not transmit them to any third-party server.
7. **Photos / Videos**: **None collected**.
8. **App Activity / Analytics**: **None collected**.
9. **Device or Other IDs**: **None collected**.
