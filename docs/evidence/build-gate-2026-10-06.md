# Windows build gate, 6 October 2026

## Authority and checkpoint

The current request is to finish the project. The supplied architecture's historical
paused/absent-tools statements are context, not a request to pause this work.
`BLUEPRINT.md` and the supplied `Steady_Merged_Android_Architecture-2.md` have
identical SHA-256 `035f15ede318b30cf747791432ffbae23e3e39e2aa13e0b7e3338b9b50124bd7`.

The imported directory had no Git history. Local checkpoint `d33cce2` preserves the
source before changes. It is a recoverable source checkpoint, not a passing build.
No repository copy or remote posting was created.

Build/setup candidate commit: `5ec62ab`. Results below apply to that code checkpoint;
the evidence/documentation commit follows it.

## Actual environment

- Windows, PowerShell; Git and Node callable.
- `java`, `adb`, and `gradle` absent from PATH; JAVA_HOME, ANDROID_HOME and ANDROID_SDK_ROOT unset.
- No JDK/SDK in inspected conventional Java/Android locations.
- User reports an available Xiaomi 14 with USB debugging possible: Android "14", build `bp2a.250605.31.a3`, HyperOS `3.0.304.0.wncinxm.c09`. These values have not been inspected over adb. The actual API/build must be read before selecting device cases.
- No device connection, SDK installation, APK assembly or device screenshot has occurred.

## Changes prepared

- AGP 8.6.1 / Gradle 8.7 candidate for SDK 35; official Gradle checksum pinned.
- ViewModel Compose dependency, AndroidX property, launcher vector, ProGuard file,
  Room schema export path, and build/key ignore rules added.
- Legacy SQLCipher dependency replaced with modern 4.9.0 candidate; tagged upstream
  Room API used and native loading added. Combined integration still unverified.
- USE_EXACT_ALARM removed; source still has no INTERNET permission. Merged manifest
  audit cannot happen until build tools are available.
- Local checked-download setup script prepared. It accepts no SDK terms and makes
  no downloads without explicit install/SDK-acceptance switches.
- README/checklist/dependency notes corrected to distinguish source from results.

## Commands and results

| Command | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain` | Exit 1: JAVA_HOME unset and no java on PATH; Gradle never starts. |
| `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain` | Exit 1: same missing Java failure. No tests executed. |
| `.\gradlew.bat :app:lintDebug --no-daemon --console=plain` | Exit 1: same missing Java failure. No lint executed. |
| `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon --console=plain` | Exit 1: same missing Java failure. No device tests executed. |
| PowerShell AST parse of `scripts/build.ps1` | Pass: no parse errors. |
| Setup preflight without SDK acceptance | Pass: refuses installation before creating `.tools/` or downloading files. |
| Setup preflight with no installed tools | Pass: identifies missing JDK. This is not a passing build. |
| XML parsing of Manifest/resources | Pass: source XML well formed. No Android resource-linking claim. |
| `git diff --check` | Pass at this checkpoint. |

The earlier Linux Gradle hang has not been reproduced here. No unavailable command
is described as passed. Publisher metadata was fetched to verify tool archive
checksums; archives were not downloaded and licence acceptance was not performed.

## Limits and next action

SDK licence agreement/install decision is pending. After tools are installed, run
the dependency/build spike before advancing feature implementation. If it fails,
report the concrete dependency error and preserve this checkpoint.

Source audit exposes unfinished backup/restore, file picker, app lock/recents,
storage failure access, persisted pause/shutdown, timer generation/boot reconciliation,
review/date selection, draft/save failures, strings and accessibility. None of these
is marked complete. There are no screenshot/test settings or device results yet.
