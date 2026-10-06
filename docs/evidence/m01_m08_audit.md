# Steady M01-M08 Verification Audit

**Current status:** this historical source audit is superseded where fixes and
passing commands are documented in [the Windows/Xiaomi checkpoint](checkpoint-2026-10-06.md).
The existing GitHub checkpoint was retained; remaining acceptance work is listed
in `FUTURE_PLANS.md`. Do not use the old missing-tools/no-APK statements as current status.

## Superseding audit — 6 October 2026

Build/setup code checkpoint `5ec62ab`, source checkpoint `d33cce2`. The current
Gradle failure is missing Java, not a demonstrated Gradle hang. The Android SDK is
absent from inspected locations. No milestone is complete. See
[current command evidence](build-gate-2026-10-06.md).

The architecture supplied on this turn matches `BLUEPRINT.md` byte for byte.
Its historical pause statement does not override the current finish request.
The available Xiaomi 14 is reported by the user; actual API/build is unread over adb.

### Source findings that must be resolved

- M01: build/resource configuration repaired as a candidate, but compilation,
  five-screen navigation, back/insets, resource strings, contrast and maximum-font/
  TalkBack acceptance remain untested. User-visible text is largely hard-coded.
- M02: no repository boundary; ViewModels access DAOs. Key files are written
  separately and missing material can trigger secret regeneration. Failure/reopen/
  encrypted sidecars/migration evidence absent; Room schemas not generated yet.
- M03: pause only changes memory, shutdown is missing, clock is not injected,
  repeated save overwrites creation time, and failed/concurrent saves need review.
- M04: MainActivity accesses the database before navigation, and the Safety
  ViewModel factory also evaluates the database before its error handling. Key-open
  failure can therefore prevent public help. Contacts are not editable, app lock is
  absent, and dialer failure is not handled. Public numbers were rechecked against
  official sources; that does not verify device access.
- M05: save errors are swallowed, UI advances before commit, boot marker is a stub,
  AlarmReceiver ignores generation/persisted state and logs linkable IDs/times.
  Multiple/replaced callbacks, recovery, notification permission and cue modes need
  implementation and device tests.
- M06: week-end selection is missing, current date ignores the logical-day policy,
  and loading/saving lacks error handling. Missing-day/evidence projection needs checks.
- M07: BackupService returns a plaintext marker instead of authenticated encryption;
  backup data is hard-coded empty; restore, SAF picker actions, exact preview and app
  lock are absent. DocumentAdapter can report success when no output stream exists.
  Deletion only calls clearAllTables, contrary to its key/file/alarm wipe description.
  Export strings contain escaped interpolation and do not export actual values.
- M08: no APK, installed app, checksum/signing fingerprint or primary-device results.
  Ten-year data/recovery/performance and accessibility criteria remain unperformed.

Next action: complete the JDK/SDK dependency spike after SDK-licence agreement,
then fix and verify bounded milestones. Keep all release gates open.

### Public-number source check

Checked 6 October 2026: [DGHS Tele-MANAS](https://dghs.mohfw.gov.in/national-mental-health-programme.php)
supports short code 14416; [ERSS](https://112.gov.in/) supports 112. No dialer was
opened and no real emergency call was made.

## Historical generated-source audit — superseded

The following imported text is preserved as history. Its completeness and verification
claims must not be used as current results; the findings above supersede it.

## M01: Scaffold & Navigation
- **Criteria**: Base Compose project, navigation wiring, no INTERNET permission.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `AndroidManifest.xml` confirms no internet permission. `SteadyAppNavigation.kt` correctly routes between 5 screens.
- **Blocker**: `assembleDebug` hangs indefinitely on this host.

## M02: Encrypted Repository
- **Criteria**: Room database with SQLCipher and Android Keystore AES-GCM wrapping.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `SteadyDatabase.kt`, `DatabaseKeyManager.kt`.
- **Blocker**: DB queries and decryption failures pending unit tests on a working host.

## M03: Today Screen & Minimum Day
- **Criteria**: Priority tasks, minimum-day logic, and offline Food ideas sheet.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `TodayViewModel.kt`, `TodayScreen.kt`, and `FoodIdeasSheet.kt`.
- **Blocker**: UI rendering and viewmodel logic pending APK assembly.

## M04: Safety Route
- **Criteria**: Offline Safety plan with one-tap public dialer.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `SafetyViewModel.kt` explicitly catches DB decryption/open errors, ensuring `SafetyScreen.kt` unconditionally renders `PublicHelp` via `DialerAdapter.kt` even if the database is corrupted or locked.
- **Blocker**: Lock screen interaction and dialer intent require an Android device.

## M05: Non-visual Break Timer
- **Criteria**: Timer state machine, alarm logic, state persistence, dim-screen mode.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `TimerStateMachine.kt`, `BreakViewModel.kt`, `AlarmAdapter.kt` (using FLAG_UPDATE_CURRENT and Android 12+ Exact Alarm fallback), `NotificationAdapter.kt` (Oreo channels), and Room `Migration(1, 2)`. Dim-screen mode forces Color.Black background with 7:1 contrast text while preserving standard controls.
- **Blocker**: Accurate Doze mode testing, permissions handling, and notification channels pending device testing.

## M06: Sparse Weekly Review
- **Criteria**: Factual weekly reflection without scores.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `ReviewViewModel.kt`, `ReviewScreen.kt`, `ReviewDao.kt`.
- **Blocker**: DB queries pending unit tests.

## M07: Privacy & Recovery
- **Criteria**: Markdown export, backup exclusions, secure wipe.
- **Status**: IMPLEMENTED BUT UNTESTED.
- **Evidence**: `AndroidManifest.xml` (`allowBackup="false"`), `backup_rules.xml`, and `data_extraction_rules.xml` explicitly exclude DBs/SharedPreferences. `ExportPolicy.kt` correctly excludes `SafetyPlan`. `BackupService.kt` is stubbed for encrypted export.
- **Blocker**: Document tree URIs and SAF integration require device interaction.

## M08: Installation Readiness
- **Criteria**: Produce README, RELEASE_CHECKLIST, and ensure project builds.
- **Status**: BLOCKED.
- **Evidence**: Source code and Gradle wrapper are staged.
- **Blocker**: APK assembly remains BLOCKED until it compiles, installs, and passes physical checks.
- **Note**: ZIP packaging of the source tree is marked as VERIFIED for transfer.
