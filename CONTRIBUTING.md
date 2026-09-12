# Contributing

Bug reports and focused pull requests are welcome.

## Development setup

Install JDK 17 and Android SDK Platform 35, Build Tools 35.0.0, and Platform Tools. Point `ANDROID_HOME` to the SDK or create `local.properties` with:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Run the same checks used by CI:

```bash
./gradlew test lintDebug assembleDebug
```

On Windows PowerShell or Command Prompt:

```powershell
.\gradlew.bat test lintDebug assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Pull requests

- Keep the app native Kotlin/Java and dependency-light.
- Preserve the current data-SIM resolution and exact read-back checks.
- Never add guessed Binder transaction numbers or permissive shell-output parsing.
- Do not add polling, a persistent service, an overlay, root-only behavior, or stored SIM selection.
- Include tests for parsing, masks, or SIM-selection logic when those areas change.
- State whether device results prove configuration, registration, or only emulator behavior.
- Run the full check command before opening a pull request.

Do not include device serials, phone numbers, subscription identifiers, or unredacted logs in issues or pull requests.
