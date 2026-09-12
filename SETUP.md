# Poco setup

Use the APK attached to the [latest GitHub release](https://github.com/Manitchahar/poco-5g-toggle/releases/latest), or build it from source using the commands in [README.md](README.md). The release APK is a sideloadable development build, not a Play Store package.

1. Install the APK. If Android says the signature conflicts with an existing build, uninstall that differently signed build first; this also removes its permissions and tile/widget placement.
2. Install and open Shizuku. Start it through Wireless debugging, or use Shizuku's USB start command while the phone is connected.
3. Open **5G Toggle** once. Allow the Phone permission, then grant the app access in Shizuku.
4. Open Quick Settings, tap **Edit**, and add the **5G** tile. Add the small home-screen widget from the launcher's Widgets menu if wanted.
5. Tap the tile or widget. The app resolves whichever SIM is carrying mobile data at that moment. It never saves a SIM choice.

The Quick Settings tile always shows `5G`: bright means 5G is allowed; grey means 5G is disabled while LTE and the existing fallback networks remain allowed. It changes only after the new policy is read back successfully. There is no loading animation.

After a reboot, Shizuku authorization normally remains but its shell service may be stopped. If the toggle reports unavailable, open Shizuku and start it again. HyperOS may require **Wireless debugging**, **USB debugging (Security settings)**, and **Install via USB** during setup. Its battery/background controls can also stop Shizuku auto-start.

For a USB install, connect and unlock the phone, accept its USB debugging prompt, check `adb devices` for a `device` entry, then run `adb install -r app/build/outputs/apk/debug/app-debug.apk` from the source checkout. When sideloading an APK directly on the phone, Android may ask you to allow installs from the file manager/browser used to open it.

Enabling 5G changes the allowed/preferred radio policy. It cannot guarantee immediate NR registration; coverage, provisioning, signal, power policy, and NSA LTE anchoring still decide what network the modem actually uses.
