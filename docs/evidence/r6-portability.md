# R6 portability checkpoint — in progress

Source checkpoint: `e082f01`. Prior expanded shell: `940394c`.
Host: Windows, local Temurin JDK 17.0.20.1+1, Gradle 8.7, SDK 35.
Device: Xiaomi 14 (23127PN0CG), Android 16/API 36, HyperOS OS3.0.304.0.WNCINXM.
Data fixtures use isolated synthetic databases. The connected-runner cleanup issue below
must be considered separately from fixture isolation.

## Commands and results

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`
  passed in the final host run: 19 domain/crypto unit tests; lint 0 errors, 54 warnings.
  The earlier test compilation failed on a cross-module nullable-property smart cast,
  was corrected, and test packaging then passed. It is not counted as a passing run.
- `adb install -r app-debug.apk` and `adb install -r -t app-debug-androidTest.apk`: Success.
- Native instrumentation runner: **OK (11 tests)**, 170.356 seconds. Includes previous eight
  checks and three portable recovery checks: all 25 organiser collections plus profile/preferences,
  route opt-in, draft/Safety exclusion, malformed types/keys/references, atomic rollback and
  interrupted imported sessions. This run predates the last historical-query corrections.
- Expanded shell checkpoint installed and launched: cold launch reported 780 ms by `am start -W`.
  This is one launch observation, not a p95 benchmark or proof of warm Today performance.
- Final host APK SHA-256:
  `9b02527ff99bb7a7995a148c0450c70b41285d4cc5479808c941d82274a28d4b`.
  Earlier 11-test device APK hash:
  `599f862d16f1db03fa524df009c19aacfcb285e3aa6b6440185a1a5eaa208716`.
- Debug signing certificate SHA-256:
  `cb5e9942c805dc176cd0e7e3d9b836e3120fb720c31ae5b7f9e78eb257e6ac56`.
  Production signing has not been chosen.
- Every packaged SQLCipher/DataStore native ELF has minimum PT_LOAD alignment 16384.
  Build-tools 34 zipalign rejects `-P 16`; that command is unavailable and is not a passed check.
  APK native entry alignment remains a separate packaging check.

## Contract and privacy

Portable payload schema 2 is typed Kotlin serialization with strict key/type/relationship checks,
duplicate-key and nesting preflight, a 25 MiB payload bound and 100,000 total-record bound.
The existing authenticated STDYBK01 AES-256-GCM envelope and PBKDF2-SHA256/600,000 contract
remain compatible; the UI reads legacy payload schema 1, with its narrower historical scope.
Route points are absent by default and require explicit scope preview. Form drafts, Safety,
contacts, credentials, keys and device live-alarm details are excluded. Private Safety is not
queried by the portable DAO. Restore replaces eligible organiser tables atomically and does
not restart imported activity. A provider may upload a selected file; Markdown is readable.

Safety editor controls are now labelled resources with contacts, preserved in-memory drafts,
save failures, follow-up note and user-reviewed action. Public Safety has no database parameter
and can be opened before onboarding or on organiser failure. Private Safety always sets secure
window handling independently of the Recents preference.

## Limits and next action

The subsequent Gradle connected command passed **13 tests**, no failures/skips, with
18.598 seconds of test execution (48 seconds including Gradle/device setup). Historical
zone/correction/reflection checks passed. The 7,300-record, ten-year fixture measured
Today query p95 6.002084 ms and water-save p95 2.383907 ms over 20 repetitions each.
These are isolated-storage measurements, not end-to-end UI response or frame benchmarks.

**Runner cleanup correction:** Gradle's connected runner uninstalled the target package
after testing. The owner package was absent afterwards. This was an avoidable test-isolation
mistake; any owner-entered records may have been removed. A reinstall was rejected by
Xiaomi with INSTALL_FAILED_USER_RESTRICTED. The owner was informed and asked whether
records had been entered and to unlock/approve the resend. Future connected tests target
the separate com.thanu.steady.qa package via the qa build type; scripts/README now use
connectedQaAndroidTest. The production com.thanu.steady package is never a cleanup target.

The owner-approved reinstall succeeded and opened MainActivity (one cold launch: 559 ms).
The isolated `:app:connectedQaAndroidTest` run subsequently passed all 13 tests in 50 seconds
including setup. `pm path com.thanu.steady` returned the same production APK path before and
after QA cleanup. The merged test manifest targets `com.thanu.steady.qa`, not the owner package.
Whether the owner had entered records before the earlier cleanup is still unanswered.

The expanded release is **not yet accepted**. UI/provider/authentication cancellation tests, private draft
process recovery, quiet-hour reminder scheduling, interval/rest/template controls, routine/card
sizing, complete P0 reconciliation, max-text/TalkBack/contrast, native ZIP alignment and broader
device delivery scenarios remain open. No private Safety screenshot is captured.
