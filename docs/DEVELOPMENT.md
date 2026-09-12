# Development environment

The repository is an ordinary Gradle Android project. Android Studio is optional; VS Code, PowerShell, Command Prompt, and Bash all work.

## Versions

| Tool | Version used |
|---|---|
| JDK | 17 |
| Gradle wrapper | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin plugin | 2.0.21 |
| Android compile SDK | 35 |
| Android Build Tools | 35.0.0 |
| Android minimum SDK | 33 |
| Android target SDK | 35 |

Install Google's Android SDK command-line tools, then use `sdkmanager` to install the required components:

```text
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Accept the SDK licenses with `sdkmanager --licenses` if prompted. Set `ANDROID_HOME` to the SDK root and `JAVA_HOME` to the JDK 17 root. Put the SDK's `platform-tools` directory on `PATH` for `adb`.

On Windows, `ANDROID_HOME` is often `%LOCALAPPDATA%\Android\Sdk` and JDK 17 may be installed by an IDE or a JDK vendor. In PowerShell, verify the environment with:

```powershell
java -version
adb version
.\gradlew.bat --version
```

On Linux/macOS, use `java -version`, `adb version`, and `./gradlew --version`.

As an alternative to `ANDROID_HOME`, put `sdk.dir=<absolute SDK path>` in an untracked `local.properties` at the project root. Use forward slashes in that file on Windows or escape backslashes.

## Compile and test

```powershell
.\gradlew.bat clean test lintDebug assembleDebug
```

```bash
./gradlew clean test lintDebug assembleDebug
./tools/test-data-sim.sh
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The Kotlin/JVM tests cover strict RAT parsing and NR/LTE mask handling; the host Java script covers active-data SIM selection. CI runs both test groups and Android lint. Neither host tests nor an emulator prove a particular modem accepts `cmd phone`; use the [device validation guide](DEVICE_VALIDATION.md) on physical hardware.

## Install on a connected Poco

1. Enable USB debugging on the phone, unlock it, and accept its ADB authorization prompt.
2. Check `adb devices` shows the phone as `device`, not `unauthorized`.
3. Run `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
4. Start Shizuku on the phone, then open the app and grant Phone and Shizuku permissions.

HyperOS may require **Install via USB** and **USB debugging (Security settings)**. Do not hard-code a device serial or SIM slot in scripts. If more than one device is attached, select the intended device with `adb -s <serial>` for the install command.

## VS Code

Open the repository folder itself. Configure VS Code's Java runtime to JDK 17 and run the Gradle commands in the integrated terminal. Editing Kotlin/XML does not require Android Studio, though Android Studio's Android tooling can provide richer previews and device inspection.

## Signing

`assembleDebug` uses the local Android debug signing key. Another machine's debug key is different, so its APK may not update an existing installation. See [RELEASING.md](RELEASING.md) before distributing builds.
