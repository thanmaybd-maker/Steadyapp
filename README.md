# Steady

An offline Android organiser for one user, scoped to Today, Break, Safety, Review
and Settings. The architecture and acceptance contract are in `BLUEPRINT.md`.

## Current status

**6 October 2026 update:** the existing checkpoint now builds; encrypted recovery
and SAF flows are implemented, and a debug APK is installed and launches on the
Xiaomi 14. Baseline `be3d20e` passed 10 unit tests, lint with no errors and 4 device
tests. Latest code `a36e6da` passed the build, 14 unit tests and lint (0 errors,
41 warnings), followed by all 4 connected regression tests. v0.1 acceptance is
still incomplete. See [current evidence](docs/evidence/checkpoint-2026-10-06.md)
and [the completion plan and future backlog](FUTURE_PLANS.md).

### Historical setup audit (superseded)

**Incomplete; no APK or verified personal-use release exists.** The imported source
contains unimplemented recovery and file-picker flows, missing access controls,
and timer/storage defects. The previous checked milestone list overstated progress.
Use synthetic data only in this development scaffold.

On this Windows host, all four required Gradle commands exit with code 1 because
Java is absent. No Android SDK was discovered. The earlier Linux Gradle hang has
not been reproduced here. See `docs/evidence/m08.md` and
`docs/evidence/m01_m08_audit.md` for the current audit.

The user has a Xiaomi 14 available for USB testing. Android/HyperOS values have
been supplied; the actual API level/build must be read through adb before testing.

## Build

The candidate configuration pins AGP 8.6.1, Gradle 8.7, Kotlin 1.9.22, Compose
compiler 1.5.10, Compose BOM 2024.02.02, Room 2.6.1/KSP 1.9.22-1.0.17, and
SQLCipher Android 4.9.0. These selections still need a successful dependency/build
spike. SDK 35 and JDK 17 are required. The wrapper checks the Gradle archive SHA-256.

Use [the Windows setup instructions](docs/build-setup.md). A prepared local setup
script can install JDK/SDK tools after explicit SDK-licence agreement. With an
existing JDK/SDK installation:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedQaAndroidTest
```

Connected checks require an authorised emulator or phone. Passing a build alone
does not establish encrypted storage, accessible interaction, recovery, or timer
reliability. Primary Xiaomi testing remains part of release acceptance.

## Privacy and scope

The intended release has no INTERNET permission, telemetry, accounts, automatic
contact actions, or medical logic. Safety fields must stay device-only and must
never appear in logs, export, portable backup, or screenshots. Use synthetic data
for development and verification. Public help must work even when storage or
authentication is unavailable.

Export, backup, lock, deletion and device behaviour are requirements still to finish
and verify, not currently working features. Do not distribute this scaffold as a
completed application.
