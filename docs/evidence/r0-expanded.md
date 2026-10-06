# R0 — expanded foundation, in progress

Specification input checkpoint: `ecfe758`; working baseline: `c498d4c`.
The authoritative repository is `thanmaybd-maker/Steadyapp`; package remains
`com.thanu.steady`. The phone was installed with version 0.1/versionCode 1,
targetSdk 35, using the host's standard Android debug signing identity. No production
signing key or store release has been identified. Preserve that identity for local
updates and define release signing before distribution.

The source pack is now tracked unchanged. `spec2-source-points.csv` indexes 527
nonempty source lines so each point can receive an explicit disposition/evidence.
Source inventory entries are provenance/reuse decisions, not instructions to merge
excluded template runtimes or invent missing source archives.

## Proven combination retained

Per technical specification section 4, a proven current combination may be retained.
JDK 17, Gradle 8.7, AGP 8.6.1, Kotlin 1.9.22, KSP 1.9.22-1.0.17, Compose BOM
2024.02.02, Room 2.6.1 and SQLCipher Android 4.9.0 remain pinned. Added Kotlin
serialization plugin 1.9.22/runtime 1.6.2 and DataStore preferences 1.1.1.
Serialization's tagged release accompanies Kotlin 1.9.21; compatibility with our
1.9.22 compiler was proven by compilation. DataStore is reserved for nonsensitive
bootstrap flags. Personal preferences and records remain in encrypted Room.

`:app:kspDebugKotlin :app:compileDebugKotlin --no-daemon --console=plain` passed.
`:app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain` passed with
the new foundation. Device migration/storage checks are next; none is marked
accepted before its result. SDK 36, native 16 KB compatibility, dependency licence
inventory, clean build identity and final signing/artifact metadata remain gates.

## Changes under verification

- Explicit organiser migration 3 to 4 adds 26 empty typed tables and indexes.
  Statements are frozen from exported Room schema 4, not generated at runtime.
- Independent encrypted private Safety store uses its own random secret and
  Keystore alias. Legacy Safety is copied, verified, then removed from organiser
  tables; source changes during copy cause refusal instead of silent loss.
- Versioned habits retain labels/targets/schedules and idempotent per-occurrence logs.
- Activity state uses persisted monotonic anchors, active segments, boot context and
  generations. Two minutes of a 25-minute session is two minutes; pause is excluded.
- Domain checks cover reboot interruption, overlapping intervals, 04:00 splitting,
  partial habits and unscheduled dates. Device checks use synthetic isolated stores.

No real user database is cleared. Public Safety, final expanded UI, lock, sensors,
portable expanded round-trip and full P0 acceptance still require implementation
and evidence. The owner selected expanded release first, then P2.

## Device results

First connected attempt stopped before executing tests: Xiaomi rejected the update
with `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. The owner asked
to resend. Both `adb install -r app-debug.apk` and `adb install -r -t
app-debug-androidTest.apk` then returned Success. No security setting was bypassed.

`adb shell am instrument -w com.thanu.steady.test/androidx.test.runner.AndroidJUnitRunner`
passed: **OK (8 tests)**, 9.016 s. The four existing recovery tests remained green;
four new foundation checks passed: schema 3 to 4 preserves encrypted preferences,
partial focus and stale completion, idempotent habit logs/historical rename, and
Safety relocation with organiser unavailable afterwards. The failed Gradle connected
command is not reported as passed. Its later source/UI additions require fresh
verification. All 19 domain/crypto unit tests passed in the prior host run.
