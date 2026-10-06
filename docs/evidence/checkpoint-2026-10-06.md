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

Code `a36e6da`, preserved with original GitHub history by merge `11b2f0a`:
`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --console=plain`
finished BUILD SUCCESSFUL in 1m 25s. All 14 unit tests passed (8 recovery,
3 logical-day including DST, 3 timer state-machine); lint had 0 errors and
41 warnings. `:app:connectedDebugAndroidTest --no-daemon --console=plain` then
finished BUILD SUCCESSFUL in 57s: all 4 encrypted storage/recovery device tests
passed on the Xiaomi running Android 16. These tests do not establish upgrade
migration or background timer delivery acceptance.

Latest debug APK SHA-256:
`5a722a12165f15c2d661a65c68d374ea74a530fb460a388b3ef853f8b570e70b`.

Timer/persistence/preference and upgrade migration device behaviour following
`be3d20e` still needs dedicated acceptance tests beyond this regression suite.
Screenshots were not captured because private Safety must never be captured.
Maximum text/display, TalkBack, screen-off/Doze/OEM timer delivery, app lock and
end-to-end SAF provider interaction have not been accepted locally. Several v0.1
requirements remain unfinished; see `FUTURE_PLANS.md` and `RELEASE_CHECKLIST.md`.

The earlier missing-Java/SDK/no-APK findings in historical evidence are superseded
by this checkpoint. Historical commands remain records of their actual runs.
