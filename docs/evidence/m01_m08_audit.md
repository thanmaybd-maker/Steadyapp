# Steady M01-M08 Verification Audit

This audit evaluates the current source code implementation against the architectural requirements. Actual on-device testing and APK assembly are marked BLOCKED.

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
