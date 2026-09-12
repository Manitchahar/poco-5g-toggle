# Architecture

## Operation flow

Every activity, widget, or tile action enters the same process-wide single-thread executor:

1. Resolve Android's current active-data subscription.
2. Map that subscription to its current logical SIM slot.
3. Read the user allowed-network policy.
4. Reject empty, unknown, or LTE-free results.
5. Resolve the active-data target again before writing.
6. Add or remove only the NR bit.
7. Write the binary mask for the resolved logical slot.
8. Read the policy back and require an exact mask match.
9. Resolve the target again and report if mobile data moved during the operation.

No subscription or slot is stored. A later tap always starts with a fresh resolution.

## Backends

`NetworkModeController` tries backends in this order:

1. `PublicTelephonyBackend` uses `TelephonyManager` for carrier-privileged or privileged builds.
2. `ShizukuBackend` uses a supported Shizuku non-daemon user service and AIDL.

The Shizuku helper accepts only read and write methods. It invokes `/system/bin/cmd` directly with a fixed argument shape. It has no generic command or shell-string interface.

## UI entry points

- `NetworkTileService`: active/grey 5G Quick Settings state; no loading animation.
- `NetworkWidgetProvider`: compact home-screen control using `goAsync()` for receiver lifetime.
- `MainActivity`: setup, permission, target, backend, mask, and verification details.

Rapid tile taps are gated. All operations share one executor so activity, widget, and tile work cannot mutate the radio policy concurrently.

## Policy model

Android represents allowed radio access technologies as a bitmask. NR is bit 19 and LTE is bit 12. The app copies the complete existing policy and changes only bit 19. It never replaces the policy with a hard-coded "4G mask" or "5G mask."

NSA remains possible because enabling NR keeps LTE, which can provide the anchor connection.

## Deliberate exclusions

- No root requirement
- No hidden Binder transaction numbers
- No Settings database mutation
- No polling, alarm, job, or resident foreground service
- No floating overlay
- No Internet permission
- No manual or persisted SIM selection
