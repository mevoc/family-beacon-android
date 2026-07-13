# Family Beacon – Product Requirements & Scope Document

**Version:** 0.1 (Draft)  
**Repository:** https://github.com/mevoc/family-beacon-android  
**License:** MIT  
**Status:** Early development – sideload distribution

---

## 1. Overview

Family Beacon is an open-source Android application that enables families to share safety signals using SMS and local device features. It is designed for mutual, transparent, opt-in use between trusted family members — not covert monitoring.

The app requires no cloud infrastructure, no backend server, and no account creation. All communication happens over SMS, making it functional even when mobile data is unavailable.

---

## 2. Problem Statement

Existing family safety apps typically require:
- Cloud accounts and backend infrastructure
- Internet connectivity
- Subscription fees
- Trust in a third-party service with sensitive location data

There is no simple, open-source, serverless option for families who want basic safety signalling — particularly useful in low-connectivity situations (hiking, remote areas, emergencies) or for families who prefer not to share data with third parties.

---

## 3. Core Principles

These are non-negotiable and inform all design decisions:

- **Explicit opt-in** – every feature must be individually activated on the device
- **No cloud, no servers** – all data stays on device or travels via SMS
- **Transparent activity log** – every triggered event is visible on the device
- **Not surveillance** – the app cannot be activated remotely or covertly
- **PIN-protected configuration** – only the device holder can change settings
- **Whitelist-only** – the app only responds to pre-approved phone numbers

---

## 4. Target Users

**Primary:** Adults in a family unit who collectively agree to use the app. Each participant installs and configures the app themselves on their own device.

**Secondary use case:** Parents installing on a child's device, with the child's knowledge, for safety purposes. Explicitly addressed in the ethics and privacy documentation.

**Not a target user:** Anyone seeking to monitor another person without their knowledge or consent.

---

## 5. Feature Scope

### V1 – Current / In Development

| Feature | Description | Trigger | Response |
|---|---|---|---|
| **SMS Location Request** | Sends device GPS location via SMS | Incoming SMS from whitelisted number with location keyword | Outbound SMS with coordinates |
| **Battery Level Alert** | Notifies when battery falls below threshold | Battery level drops below configured % | Outbound SMS to whitelisted numbers |
| **Panic Alert** | Triggers audible alarm + vibration on device | Incoming SMS from whitelisted number with panic keyword | Alarm on device; optional SMS acknowledgement |
| **Geofence Notification** | Sends SMS when device enters or exits a defined zone | Geofence boundary crossed | Outbound SMS to whitelisted numbers |
| **Event Log** | Local, tamper-visible log of all triggered events | All feature triggers | Stored in local Room database, viewable in-app |

### Feature Configuration (all V1)

- Each feature individually enabled/disabled
- PIN protection on all configuration changes
- Whitelist management (add/remove trusted numbers)
- Per-feature settings (e.g. battery threshold, geofence radius)

### V2 – Planned (Out of Scope for V1)

- SMS-based invitation and pairing handshake between devices
- Mutual contact model (both parties confirmed)
- Exportable event log
- Role-based profiles (e.g. child vs. parent device behaviour)
- F-Droid submission

---

## 6. Technical Stack

| Component | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Database | Room (SQLite) |
| Location | Google Play Services Location API |
| Background work | Android Services / BroadcastReceivers |
| Build system | Gradle (Kotlin DSL) |
| CI/CD | GitHub Actions |
| Distribution | GitHub Releases (signed APK) |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 35 |

---

## 7. Architecture Notes

- **No network calls** – the app makes no HTTP requests. The only outbound communication is SMS.
- **SMS handling** – incoming SMS filtered by whitelisted numbers before any action is taken. BroadcastReceiver listens for `SMS_RECEIVED`.
- **Background operation** – features that require background activity (geofencing, battery monitoring) use foreground services with visible notifications to comply with Android background restrictions.
- **Data model** – all settings and event log entries stored locally via Room. No SharedPreferences for sensitive config (PIN, whitelist).

---

## 8. Permissions Required

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` | Listen for incoming SMS triggers |
| `SEND_SMS` | Send location/alert responses |
| `ACCESS_FINE_LOCATION` | GPS coordinates for location requests |
| `ACCESS_BACKGROUND_LOCATION` | Geofencing while app is in background |
| `FOREGROUND_SERVICE` | Keep geofence and battery monitors alive |
| `RECEIVE_BOOT_COMPLETED` | Re-register services after device restart |
| `READ_PHONE_STATE` | (if needed for SIM identification) |

All permissions are requested with in-app explanation before the system dialog is shown, consistent with the consent flow.

---

## 9. Consent & Ethics Model

### Consent Activity
An explicit consent screen is shown on first launch, before any permissions are requested. It explains:
- What the app does and does not do
- Which permissions are needed and why
- That features are opt-in and can be disabled at any time
- That configuration is PIN-protected

### What the App Explicitly Does Not Do
- No remote activation by another party
- No silent background installation or auto-start without user action
- No location history or tracking – only event-triggered responses
- No data sent to any server, analytics service, or third party

### Ethics Documentation
`ETHICS.md` and `PRIVACY.md` are maintained at the repository root and linked from the README and the in-app about screen.

---

## 10. Distribution Strategy

### V1 – Sideload via GitHub Releases
- Signed release APK built automatically via GitHub Actions on git tag push
- APK attached to GitHub Release page
- README includes clear install instructions (enable unknown sources, download, install)

### Future
- F-Droid submission (aligns with open source / privacy-first ethos)
- Google Play Store submission (requires additional compliance work: hosted privacy policy URL, prominent disclosure screen, Play Store declaration for SMS + location permissions)

---

## 11. Build & Release Pipeline

```
git tag v0.x.x
    ↓
GitHub Actions (build-release job)
    ↓
Decode keystore from KEYSTORE_BASE64 secret
    ↓
./gradlew assembleRelease (signed via env vars)
    ↓
softprops/action-gh-release uploads APK
    ↓
GitHub Release published with APK attached
```

Secrets required in GitHub repo:
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

---

## 12. Out of Scope

The following are explicitly not part of Family Beacon:

- iOS support (SMS API unavailable on iOS; would require fundamentally different architecture)
- Web or desktop interface
- Cloud sync or backup of event logs
- Real-time location sharing or tracking
- Group/family management features
- Push notifications (by design – SMS only)
- Any form of advertising or monetisation

---

## 13. Open Questions / Decisions Pending

| Question | Notes |
|---|---|
| Invitation/pairing flow | Scoped to V2; V1 uses manual whitelist configuration |
| Minimum battery threshold default | TBD – suggest 20% |
| Geofence zone management UI | How many zones, named zones? |
| PIN recovery mechanism | Currently unclear – what happens if PIN is forgotten? |
| Log retention period | Should logs auto-purge after N days? |
| README screenshots | Should be added before first public release |

---

## 14. Versioning

Follows semantic versioning (`MAJOR.MINOR.PATCH`). Until v1.0.0, the app is considered pre-release / beta and distributed via sideload only.

Current version: `1.0` (versionCode 1, versionName 1.0) – pre-release.

---

*This document is a living draft. It should be updated as features are implemented and decisions are made.*
