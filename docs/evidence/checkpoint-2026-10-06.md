# Windows / Xiaomi checkpoint — 6 October 2026

This continues the existing GitHub/Gemini codebase. The imported local tree matches
GitHub `42388fb` apart from the executable bit on `gradlew`.

## Verified baseline

Code checkpoint `be3d20e`:

| Command / check | Result |
| --- | --- |
| `scripts/build.ps1 -InstallTools -AcceptAndroidSdkLicense` | Local JDK 17 and SDK installed. Initial wrapper download timed out; official Gradle 8.7 archive was subsequently downloaded and its pinned SHA-256 verified. |
| `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` | BUILD SUCCESSFUL; 10 unit tests passed; lint 0 errors, 42 warnings. |
| `:app:connectedDebugAndroidTest` | BUILD SUCCESSFUL; 4 connected tests passed on primary Xiaomi. |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Success after user enabled Install via USB. |
| `adb shell am start -W -n com.thanu.steady/.MainActivity` | Status ok; process launched. |

Connected tests use isolated synthetic encrypted databases, including CRUD/reopen,
wrong key rejection, canary absence from storage, authenticated portable restore,
Safety exclusion/preservation, stopped imported timers, failed-import rollback and
strict malformed payload rejection. Tests do not clear the production database.

Actual adb properties: Xiaomi 14, model 23127PN0CG; Android 16, API 36;
build BP2A.250605.031.A3; HyperOS OS3.0.304.0.WNCINXM.

## Current checkpoint limits

Timer/persistence/preference and migration changes following `be3d20e` are under
verification at this commit. Do not attribute baseline results to unverified changes.
Screenshots were not captured because private Safety must never be captured.
Maximum text/display, TalkBack, screen-off/Doze/OEM timer delivery, app lock and
end-to-end SAF provider interaction have not been accepted locally. Several v0.1
requirements remain unfinished; see `FUTURE_PLANS.md` and `RELEASE_CHECKLIST.md`.

The earlier missing-Java/SDK/no-APK findings in historical evidence are superseded
by this checkpoint. Historical commands remain records of their actual runs.
