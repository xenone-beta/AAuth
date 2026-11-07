# AAuth

[![CI](https://github.com/YOUR_USERNAME/AAuth/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/AAuth/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An offline, open-source two-factor authenticator (TOTP) for Android, built with Jetpack Compose and Material 3 (Material You).
No accounts, no cloud, no analytics: your secrets never leave the device.

> Replace `YOUR_USERNAME` in the badge URLs above after you push the repository.

## Features

- Time-based one-time codes (RFC 6238) with SHA-1, SHA-256 and SHA-512, 6 to 8 digits, custom period
- Add accounts by **scanning a QR code** (CameraX + ML Kit) or by typing the setup key
- **Import from Google Authenticator** by scanning its "Export accounts" QR code
- Rename accounts (service and account name) at any time
- Secrets encrypted with an **AES-256-GCM key in the Android Keystore**
- Optional **app lock** with biometrics or the device screen lock, or with a **separate app PIN** (4 to 8 digits,
  salted PBKDF2, escalating delay after repeated wrong tries)
- **Home screen widget**: codes stay hidden until you tap a row, then hide again when the 30 s window ends
- Screenshots blocked and codes hidden in the app switcher (can be turned off)
- Codes copied to the clipboard are flagged as sensitive on Android 13+
- **Encrypted backups**: export and import a password-protected file (PBKDF2-HMAC-SHA256 + AES-256-GCM)
- Material 3 UI with dynamic colors, light/dark theme, English and Russian

## Tech stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room (KSP), CameraX, ML Kit Barcode Scanning,
AndroidX Biometric, Jetpack Glance (widget), WorkManager, Android Keystore. No DI framework: a small
hand-rolled container is enough.

<!--
## Screenshots

Replace this comment with real screenshots taken on a device (see docs/screenshots/README.md):

| Home | Add account | Settings |
| --- | --- | --- |
| <img src="docs/screenshots/home.png" width="240"> | <img src="docs/screenshots/add.png" width="240"> | <img src="docs/screenshots/settings.png" width="240"> |
-->

## Screenshots

Coming soon. To take them yourself, switch off **Settings, Protect screen content** first, otherwise Android
shows a black screen.

## Project layout

```
app/src/main/java/dev/aauth/app/
├── core/    TOTP/HOTP, Base32, otpauth:// and Google migration parsers, Keystore and backup crypto
├── data/    Room entity/DAO, repository, settings, backup JSON format
├── ui/      Compose screens: home, scan, manual entry, settings, lock; Material 3 theme
├── MainActivity.kt, AAuthApp.kt (navigation), MainViewModel.kt
app/src/test/  Unit tests: RFC 6238 / RFC 4226 / RFC 4648 vectors, parsers, backup crypto
```

## Build

Requires JDK 17 and the Android SDK (Android Studio sets both up).

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

Or just open the folder in Android Studio and press Run.

## CI/CD with GitHub Actions

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `ci.yml` | push to `main`, pull requests | unit tests, lint, debug APK as a downloadable artifact |
| `release.yml` | tag `v*` (for example `v1.0.0`) | tests, release APK, GitHub Release with the APK attached |

Publish a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Signing release builds (recommended)

Without secrets the release APK is signed with the debug key: it installs fine, but you cannot ship updates
signed with a stable key. To use your own key:

```bash
keytool -genkeypair -v -keystore release.jks -alias aauth -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks   # copy the output
```

Then add these repository secrets (Settings, Secrets and variables, Actions):

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the base64 string from above |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `aauth` |
| `KEY_PASSWORD` | key password |

Never commit the `.jks` file (it is in `.gitignore`).

## Security model

- Secrets are stored in a local Room database, encrypted with a non-exportable Keystore key. A copy of the
  database file is useless on another device.
- Android cloud backup and device transfer are disabled for the app data for the same reason. Use the
  encrypted export to move accounts between devices.
- Backup files are only as strong as the password you choose. Keep the file and password safe.
- App lock fails open if the device has no screen lock and no app PIN is set, because there is nothing to
  authenticate with.
- With an app PIN set, only biometrics are offered next to it (not the device screen lock), so knowing the
  phone's unlock code does not bypass the app PIN.
- PIN throttling uses the wall clock, so changing the system time can shorten a delay. It slows down guessing
  but is not a substitute for a strong device lock.
- The widget never shows a code while the app lock is on: tapping a row opens the app instead. With the lock
  off, a revealed code is visible on the home screen until its 30 s window ends.

## Roadmap

- Reorder accounts, search, folders
- Hide codes in the app until tapped
- HOTP and Steam Guard support
- Tablet and foldable layouts

## License

[MIT](LICENSE)
