# Releasing

The app currently ships as a sideloadable **debug-signed preview**. It has no Play Store signing setup.

## Build and verify

```powershell
.\gradlew.bat clean test lintDebug assembleDebug
& "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose --print-certs .\app\build\outputs\apk\debug\app-debug.apk
Get-FileHash .\app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
```

On Bash, use `./gradlew` and the corresponding `apksigner` and `sha256sum` commands.

The APK lives at `app/build/outputs/apk/debug/app-debug.apk`.

## Signing continuity

Android only installs an update over an existing app when the package name and signing certificate match. Local debug keys differ by computer; GitHub Actions runners create an ephemeral debug key. Therefore a CI artifact is useful for trying the app but is **not** a reliable in-place update for the attached release APK.

For the current preview series, build future releases with the same private development signing key as the first release. Do not commit the key or its password. Record the signing-certificate SHA-256 fingerprint and APK SHA-256 hash in every GitHub release. If the key is unavailable, explain that users must uninstall the old build before installing a differently signed one and regrant permissions/re-add the tile and widget.

Before changing radio behavior in a release, check the target firmware's `cmd phone help`, save the original policy, prove both directions with exact read-backs, and restore the original policy. A successful emulator test does not establish physical modem behavior.
