# R6 portability checkpoint — in progress

Source checkpoint: `e082f01`. Prior expanded shell: `940394c`.
Host: Windows, local Temurin JDK 17.0.20.1+1, Gradle 8.7, SDK 35.
Device: Xiaomi 14 (23127PN0CG), Android 16/API 36, HyperOS OS3.0.304.0.WNCINXM.
All data checks use isolated synthetic databases; the owner database is not cleared.

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

The expanded release is **not yet accepted**. The next connected run adds old-zone history
and a synthetic ten-year workload. UI/provider/authentication cancellation tests, private draft
process recovery, quiet-hour reminder scheduling, interval/rest/template controls, routine/card
sizing, complete P0 reconciliation, max-text/TalkBack/contrast, native ZIP alignment and broader
device delivery scenarios remain open. No private Safety screenshot is captured.
