<p align="center"><img src="docs/assets/logo.svg" width="112" alt="5G Toggle logo"></p>

# 5G Toggle for Poco

A small, native Android app with a **Quick Settings tile** and **home-screen widget** for the SIM currently carrying mobile data. Tap once to disable 5G; tap again to allow 5G. No SIM picker, root, polling, floating overlay, or background service is required.

The tile always says **5G**: bright when enabled, grey when disabled. It keeps its current appearance while an operation runs and updates only after the phone confirms the new policy. There is no loading animation.

> **Device scope:** Tested on a Poco F7 running Android 16 / HyperOS `OS3.0.303.0.WOLINXM`. Other devices and firmware may expose different `cmd phone` behavior. This app changes the *allowed/preferred network policy*, not guaranteed live 5G registration.

## Download and install

1. Download the APK from the [latest release](https://github.com/Manitchahar/poco-5g-toggle/releases/latest) and install it. For a development build, download the APK from the latest successful [Android CI run](https://github.com/Manitchahar/poco-5g-toggle/actions/workflows/android.yml) or [build it yourself](#build-from-source).
2. Install and start [Shizuku](https://shizuku.rikka.app/guide/setup/) using Wireless debugging or USB debugging. Root is not needed.
3. Open **5G Toggle**, allow **Phone** permission, tap **Grant Shizuku permission**, and approve the request in Shizuku.
4. Add **5G** to Quick Settings through **Edit**. Optionally add the compact **5G Toggle** widget from the launcher’s Widgets menu.
5. Tap the tile or widget. The app reads whichever SIM is handling mobile data on *that tap*.

Detailed setup, update, and reboot instructions are in [SETUP.md](SETUP.md).

**Updating:** Release APKs are signed with a development key. Android requires the same signing certificate for in-place updates. Installing an APK built with your own debug key may require uninstalling the release build first; that removes the app’s permission grants and tile/widget placement. The release page lists the signing-certificate fingerprint so you can verify subsequent updates. A CI artifact is also signed with its runner’s ephemeral debug key and is not guaranteed to update an existing install.

## What a tap changes

Android stores user-allowed radio access technologies as a bitmask. This app copies the current policy, changes **only the NR bit**, and requires an exact read-back:

| Tile | Policy change | Kept intact |
|---|---|---|
| Grey 5G | Clear NR | LTE and every existing fallback type |
| Bright 5G | Set NR | LTE and every existing fallback type |

Keeping LTE allows LTE-anchored NSA 5G when the carrier and network support it. Enabling NR does **not** guarantee that the modem will immediately register on NR. Coverage, provisioning, signal, power policy, and the LTE anchor still matter.

The app rejects empty or unrecognized policies, a policy without LTE, command failures, a changed data-SIM target, and a read-back that does not match the exact requested mask. It never persists a target subscription or slot.

## Build from source

Requirements:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0
- Android SDK Platform Tools (`adb`)
- Windows PowerShell/Command Prompt, or a Bash terminal on Linux/macOS

Android Studio is optional. In VS Code or any terminal, set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK. Alternatively, create an **untracked** `local.properties` in the project root with `sdk.dir=/absolute/path/to/Android/Sdk`. The Gradle 8.9 wrapper downloads Gradle and dependencies on first use.

Windows PowerShell:

```powershell
git clone https://github.com/Manitchahar/poco-5g-toggle.git
cd poco-5g-toggle
.\gradlew.bat test lintDebug assembleDebug
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Linux/macOS/Git Bash:

```bash
git clone https://github.com/Manitchahar/poco-5g-toggle.git
cd poco-5g-toggle
./gradlew test lintDebug assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If `adb devices` says `unauthorized`, unlock the phone and accept the USB debugging prompt. On HyperOS, **Install via USB** and **USB debugging (Security settings)** may also be required for local installs. Keep `local.properties`, signing keys, and device logs out of Git.

The APK is at `app/build/outputs/apk/debug/app-debug.apk`. CI runs the same Gradle checks and uploads its debug APK as a temporary workflow artifact. A debug APK is for sideloading and testing; it is not a Play Store release. See the [development environment guide](docs/DEVELOPMENT.md) for SDK installation, VS Code setup, and signing details.

## Verified behavior and limits

On the reference Poco F7, `cmd phone` accepted a **logical SIM slot** with `-s`, a **binary** write mask, and returned pipe-separated RAT names. An active-data policy round trip read back:

```text
5G allowed:  GPRS|EDGE|UMTS|HSDPA|HSUPA|HSPA|LTE|HSPA+|GSM|TD_SCDMA|LTE_CA|NR
5G disabled: GPRS|EDGE|UMTS|HSDPA|HSUPA|HSPA|LTE|HSPA+|GSM|TD_SCDMA|LTE_CA
restored:    GPRS|EDGE|UMTS|HSDPA|HSUPA|HSPA|LTE|HSPA+|GSM|TD_SCDMA|LTE_CA|NR
```

The Quick Settings tile was tapped on the physical phone in both directions; independent ADB reads confirmed the policy. A real temporary-data handover between both installed SIMs was **not** forced, though the selection/revalidation logic has host tests. Emulator testing covers no-SIM behavior, not modem switching. See [device validation](docs/DEVICE_VALIDATION.md) and the [test report](TEST_REPORT.md).

## How it works

The app first attempts Android’s `TelephonyManager` API, which only succeeds for a carrier-privileged or privileged build. A normal sideloaded APK uses a [Shizuku](https://github.com/RikkaApps/Shizuku-API) AIDL user service running as the shell user. That helper invokes only typed `/system/bin/cmd phone` read/write calls. It does not use root, a generic shell command, hidden Binder transaction numbers, or Settings database keys.

The app resolves the current mobile-data subscription and maps it to a logical slot for **every** operation, including temporary data switching. It checks the target again before the write and after verification. All entry points use one process-wide executor to prevent overlapping writes.

The exact flow and failure handling are in [ARCHITECTURE.md](docs/ARCHITECTURE.md). Reference APIs: [Android TelephonyManager](https://developer.android.com/reference/android/telephony/TelephonyManager#setAllowedNetworkTypesForReason(int,%20long)), [AOSP TelephonyShellCommand](https://android.googlesource.com/platform/packages/services/Telephony/+/master/src/com/android/phone/TelephonyShellCommand.java), and [Android Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles).

## Reboot and troubleshooting

Shizuku’s shell service may stop after a reboot. If the tile or widget reports unavailable, open Shizuku and start it again. HyperOS wireless-debugging and battery restrictions can prevent automatic restart. The app itself uses no polling or resident service.

- **Tile is grey:** 5G is disabled in the allowed policy; LTE and fallback types remain.
- **Tile is unavailable/error:** Open the app for the detailed message. Check Phone permission and that Shizuku is running and has authorized the app.
- **5G is bright but status bar shows 4G:** The policy allows NR; actual registration still depends on the network.
- **Install fails with signature mismatch:** Uninstall the differently signed build, then install this APK and grant permissions again.
- **No data SIM found:** Ensure a SIM is active for mobile data. The app will not guess a target.

Use the [read-only device probe](docs/DEVICE_VALIDATION.md) to inspect a firmware before running any policy write.

## Project

Native Kotlin/Java, minimum Android 13 (API 33), no Internet permission. See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and the [MIT license](LICENSE).
