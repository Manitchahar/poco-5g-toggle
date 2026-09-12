5G Toggle preview for Poco F7. This is a sideloadable, debug-signed APK.

- Quick Settings tile: one 5G icon, bright when enabled and grey when disabled, with no loading animation.
- Compact home-screen widget and setup activity.
- Targets the SIM currently carrying mobile data on each operation.
- Changes only the NR allowed-network bit, preserving LTE and the existing fallback policy.
- Uses the public Telephony API when privileged; otherwise uses a supported Shizuku AIDL user service without root.
- Requires an exact policy read-back after each write.

Tested on a Poco F7 with Android 16 and HyperOS `OS3.0.303.0.WOLINXM`. On-screen tile taps produced independent policy reads of `906119 -> 381831 -> 906119`, restoring the original configuration. This establishes configuration behavior; it does not guarantee live NR registration.

Install Shizuku, start its service, install the APK, grant Phone and Shizuku access, then add the 5G tile in Quick Settings. See the README and SETUP.md for full instructions.

This APK uses a development signing key. An APK built on another computer or by CI may have a different certificate and may not install over it. The attached APK's SHA-256 and signing-certificate fingerprint are listed below.

APK SHA-256: `1C5BFCC41DBF48AEFF7F0D5813D42FD5FB78A8B98AF9A886A5E1F5290ABC58F9`

Signing certificate SHA-256: `c35767b4885b6e2c499636720d715eacaccb5c292b5facc2ef7d7ddaeaf0d303`
