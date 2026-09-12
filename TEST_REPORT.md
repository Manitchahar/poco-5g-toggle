# Test report

Test date: 2026-09-13 (Asia/Kolkata)

The test APK's SHA-256 is recorded with its release asset. GitHub Actions builds use a separate temporary debug signing key.

## Verified on the Poco F7

- Device: Poco F7, model `25053PC47I`, codename `onyx`.
- Installed firmware: Android 16/API 36, HyperOS `OS3.0.303.0.WOLINXM` (the phone is now on HyperOS 3, although the original request named HyperOS 2).
- Current mobile-data target was resolved afresh as logical slot 0, Jio. The internal subscription ID is intentionally omitted from this public report.
- `cmd phone help` confirms the plural `get/set-allowed-network-types-for-users` commands. `-s` is the logical slot, writes use a binary mask, reads return RAT names, and a successful write returns `set-allowed-network-types-for-users completed`.
- Original user policy was saved before any write: decimal `906119`, with NR present.
- 5G disabled read-back: decimal `381831`; NR absent while LTE and every existing fallback RAT remained.
- 5G enabled read-back: decimal `906119`; NR restored while LTE and every existing fallback RAT remained.
- The activity toggled 5G through the Shizuku user-service route and displayed exact before/after masks.
- The installed Quick Settings tile was tapped on-screen in both directions. Independent ADB reads showed `906119 -> 381831 -> 906119`. This succeeded on an icon build; the final source revision changes only launcher-resource packaging and removes an unused drawable, with no Kotlin, Shizuku, telephony, tile, or widget logic changes.
- An immediate tap shortly after reopening Quick Settings also passed. Rapid duplicate taps are gated and operations run on one process-wide executor.
- Final phone policy is restored to the original `906119` state with NR enabled.
- The tile is in the requested lower-right position. It always shows `5G`: bright when enabled and grey when disabled. It has no loading animation or intermediate icon.
- The adaptive launcher icon was installed and visually inspected in the HyperOS app drawer. Its 5G mark, green indicator, circular mask, and label render correctly.

These reads prove allowed/preferred configuration. They do not prove that the modem is currently registered on NR. The final active screenshot happened to show Jio True5G, but coverage, provisioning, radio conditions, LTE anchoring, and power policy can still leave registration on LTE after NR is enabled.

## Build and automated checks

- Windows terminal build: `gradlew.bat clean test assembleDebug lintDebug --no-daemon` — **BUILD SUCCESSFUL**. Later launcher-resource edits also passed `test assembleDebug lintDebug`.
- Android lint: **0 errors, 0 warnings**.
- Kotlin/JVM mask and parser tests: debug and release suites passed with no failures.
- Host Java active-data selection checks: **9/9 passed**.
- Manifest/resource merge, AIDL generation, Kotlin/Java compilation, APK packaging, and debug signing passed.
- Shell scripts passed Bash syntax checks; the PowerShell probe parsed without syntax errors.
- The debug APK installed successfully with `adb install -r`.
- Emulator API 35 behavior check: app starts without a SIM, reports unavailable rather than guessing a target, and the tile lifecycle does not crash. Emulator results are not counted as modem proof.

## Implementation checks

- The active data subscription and logical slot are resolved on every read and every toggle; no target SIM ID is stored.
- The target is checked again before the write and after verification. A detected data-SIM change produces an error.
- Only the NR bit is changed. Empty policies, unknown RAT output, policies without LTE, failed commands, and mismatched read-backs are rejected.
- The parser accepts only documented RAT tokens and exact successful set output; it does not scrape arbitrary numbers.
- The public Telephony API is attempted first. On this normal sideloaded build, Shizuku supplies the required shell identity through its supported AIDL user-service API.
- No hidden Binder transaction numbers, root, Settings database edits, `sh -c`, polling, alarms, jobs, foreground services, overlays, Internet permission, or saved SIM selection are used.
- The widget receiver uses `goAsync()` and finishes its pending result after asynchronous work completes.

## Remaining physical-device unknowns

- A real temporary-data handover between both installed SIMs was not forced. The selection and revalidation paths are covered by nine host tests, while physical proof used the data SIM active during testing.
- Reboot behavior was not destructively tested. Shizuku authorization should remain, but the Shizuku shell service may need to be started again after a reboot depending on HyperOS wireless-debugging and background restrictions.
