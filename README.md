# Steady

Steady is an offline native Android organiser for one owner. The expanded application
has Today, Health, Focus, Review and Settings, with independent public Safety help.
The existing codebase is being completed against the specification in
[Steady_Documentation](Steady_Documentation/README.md); it has not been rewritten.

## Status — 6 October 2026

The Windows JDK/Android SDK are installed locally. The app builds and the earlier
owner APK is installed on the Xiaomi 14 (actual Android 16/API 36). Expanded
storage, task/habit history, actual-time focus/manual workouts, health logs,
platform authentication and organiser-only encrypted backup/SAF flows exist.

Checkpoint `862b8d1` adds interval/rest/template workflows, encrypted private
editing drafts, quiet-hour reminders, routine anchors, historical corrections,
reviewed food references and scope/draft fixes. Host checks passed 26 unit tests
and lint with zero errors. Two additional public-help failure/auth-cancellation
checks passed on the isolated QA package. The double-text editor layout is still
under repair; passing these checks does not certify every specification point.

See [checkpoint evidence](docs/evidence/r7-workflow-checkpoint.md) and
[remaining acceptance work and P2 plans](FUTURE_PLANS.md). The expanded release is
not yet accepted. P1 sensors/GPS/interception/audio are gated and currently deferred.
P2 tools remain subsequent work. No fabricated measurements or private fixtures ship.

## Build and test

Use [Windows setup](docs/build-setup.md). The user authorized the local setup:

```powershell
.\scripts\build.ps1 -InstallTools -AcceptAndroidSdkLicense
.\scripts\build.ps1 -ConnectedTests
```

The script supplies the local JDK 17/SDK paths; neither must be on global PATH.
Direct Gradle use requires JAVA_HOME and ANDROID_HOME configured. Host verification:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedQaAndroidTest
```

**Connected tests must use QA.** Gradle runner cleanup can uninstall its target.
The QA build uses `com.thanu.steady.qa`, separate from the owner package
`com.thanu.steady`. Never run connectedDebugAndroidTest against owner records.
The mistaken earlier cleanup and its possible data impact are recorded in
[r6 evidence](docs/evidence/r6-portability.md); whether records were entered is unknown.

Install/update the owner package only with `adb install -r` after validating the
APK; keep its package/signing identity. Xiaomi may require an unlocked phone and
acceptance of its USB installation prompt. No release signing key is configured.

## Privacy and portability

The offline APK has no INTERNET permission, telemetry, accounts, automatic contact
actions or medical logic. SQLCipher encrypts organiser and separate private Safety
storage with protected random keys. Public help does not require either store or
app authentication. Platform backup is disabled; explicit organiser portability
uses an authenticated encrypted archive. Private Safety, contacts, keys and live
alarm tokens are excluded from ordinary files and screenshots.

Markdown exports are explicit, scoped and previewed. A document provider may sync
selected files externally. See [recovery contract](docs/recovery-format.md),
[dependency inventory and notices](docs/dependencies.md) and milestone evidence.
