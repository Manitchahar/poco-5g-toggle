# Security policy

## Supported version

Security fixes currently target the latest release.

## Reporting a vulnerability

Use GitHub's private vulnerability reporting feature if it is available for this repository. Otherwise contact the repository owner privately before opening a public issue.

Do not publish device identifiers, phone numbers, full subscription dumps, or unredacted logs.

## Trust boundary

The app has no Internet permission and does not use root. It asks Shizuku to run a narrowly typed `/system/bin/cmd phone` command as Android's shell user. The helper does not accept arbitrary shell text and does not invoke `sh -c`.

Installing an APK from an untrusted fork can change that behavior. Review source changes and build from source when provenance matters.
