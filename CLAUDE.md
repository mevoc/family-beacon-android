# Family Beacon

An open-source Android app letting families share safety signals over **SMS and local device
features** — so it still works when mobile data doesn't. Trusted contacts can request status
(location, battery) from a whitelisted number.

**No cloud services. No servers. No accounts.** Everything is SMS + on-device.

Repo: `github.com/mevoc/family-beacon-android` · MIT · sideload/APK distribution.

---

## The ethical line — read this first

**Family Beacon is NOT designed for covert monitoring.** This is the product's defining
constraint, not a disclaimer. See `ETHICS.md` and `PRIVACY.md`.

- **Explicit opt-in for every feature**, individually toggleable.
- The device user **must be aware of what the app does**. No silent capability.
- Sensitive actions require **explicit consent**; config changes are protected by the device
  lock (`AuthHelper`, biometric).
- The activity log is **visible to the device user** (`EventsActivity`) — transparency is
  enforced in the UI, not just promised.
- The app can be disabled or uninstalled at any time.

**Any change that makes the app harder for its own user to see, disable, or uninstall is a
change in the wrong direction — raise it, don't implement it.** A silent-mode feature is not
a feature request; it turns this into stalkerware.

---

## Features

SMS location request (send `POS` from a whitelisted number) · battery level alerts ·
emergency alert (audible + vibration) · geofencing (safe zones) · local event log.
All individually enable/disable-able.

---

## Code layout

`app/src/main/java/io/github/mevoc/familybeacon/`

| Package | Contents |
| --- | --- |
| `data/` | Room: `AppDatabase`, `LogDao`, `LogEntry`, `EventLogger` — the transparent activity log. |
| `receiver/` | `SmsReceiver` (inbound commands), `BatteryReceiver`, `GeofenceReceiver`. |
| `service/` | `LocationFgService` (foreground location), `PanicService`. |
| `geofence/` | `GeofenceHelper` — safe-zone registration. |
| `ui/` | Activities: `Main`, `Consent`, `SafeZones`, `Whitelist`, `MapPicker`, `Events` + `theme/`. |
| `util/` | `SmsUtil`, `PermissionUtil`, `AuthHelper` (biometric/device-lock), `Prefs`, `FeaturePrefs`, `ContactStore`, `SafeZoneStore`, `LocationUtil`, `LocationCache`. |

**Note the mixed UI:** XML layouts (`res/layout/activity_*.xml`) alongside a Compose
`ui/theme/`. Match whatever the file you're editing already uses rather than converting it
in passing.

Localised **Swedish** (`res/values-sv/`) — keep `values/` and `values-sv/` in sync when adding
strings.

---

## Stack

Kotlin 2.0.21 · AGP 8.13.2 · Compose BOM 2024.11.00 · Room 2.6.1 (via KSP) · coroutines ·
androidx biometric · appcompat + material. Versions are centralised in
`gradle/libs.versions.toml` — **add dependencies there, not inline in `build.gradle.kts`.**

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew test                 # unit tests
./gradlew connectedAndroidTest # instrumented (needs a device/emulator)
./gradlew lint
```

CI: `.github/workflows/android.yml`.

---

## Docs

- `docs/family-beacon-prd-0.2.md` — PRD. (Note: filename says 0.2; the document header says
  "Version 0.1 (Draft)". Worth reconciling.)
- `ETHICS.md`, `PRIVACY.md` — **normative**, not marketing copy.

---

## Testing reality

SMS receipt, geofence triggers, Doze and OEM battery-killers (Samsung especially) cannot be
validated on an emulator. The real acceptance test is: **does it still respond to a `POS`
after the phone has been idle in a pocket for two days?**
