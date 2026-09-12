# Device validation

The app changes allowed/preferred network configuration. A successful configuration read-back does not prove that the modem is currently registered on NR.

## Read-only probe

Connect one unlocked physical Android device with USB debugging enabled:

```powershell
.\tools\probe-hyperos.ps1 probe
.\tools\probe-hyperos.ps1 read
```

On Bash:

```bash
./tools/probe-hyperos.sh probe
./tools/probe-hyperos.sh read
```

The scripts resolve the active-data subscription and logical slot afresh. They reject ambiguous device selection and unexpected RAT output.

## Destructive proof with automatic restoration

The following command temporarily disables NR, verifies the exact read-back, and restores the original policy in a `finally`/exit-trap path:

```powershell
.\tools\probe-hyperos.ps1 proof
```

```bash
./tools/probe-hyperos.sh proof
```

Read the script before running it. Start with NR enabled. Keep the device connected until restoration completes.

## Verified reference device

| Item | Result |
|---|---|
| Device | Poco F7 (`25053PC47I`, `onyx`) |
| OS | Android 16 / API 36 |
| Firmware | HyperOS `OS3.0.303.0.WOLINXM` |
| Command slot argument | Logical SIM slot |
| Write encoding | Binary mask string |
| Read format | RAT names separated by `|` |
| Success text | `set-allowed-network-types-for-users completed` |
| NR disabled | `906119 -> 381831` |
| NR restored | `381831 -> 906119` |

LTE and every existing fallback RAT remained present in both directions. Actual physical temporary-data handover between both installed SIMs was not forced during this reference run.

## Useful failure evidence

When reporting a firmware incompatibility, include redacted output from:

```bash
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.incremental
adb shell cmd phone help
```

Remove serial numbers, phone numbers, carrier account details, and subscription identifiers before posting.
