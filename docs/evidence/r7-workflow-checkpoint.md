# Expanded workflow checkpoint — 6 October 2026

Parent: `fc76016`. This checkpoint retains the existing application and organiser
schema 4. Private Safety schema 2 has an explicit 1→2 migration for encrypted
editing drafts; neither private plans nor drafts enter organiser portability.

Added manual interval phases, workout templates/sets/rest, optional effort,
routine anchors, quiet-hour reminder budgeting, note/export scope corrections,
editable care routines, historical corrections and personalization controls.
All fixtures are synthetic. Device checks target `com.thanu.steady.qa` only.

## Verification at checkpoint

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: successful on the
  local JDK 17 / SDK 35 toolchain; 26 unit tests passed, lint had zero errors.
- Connected QA suite: 20 tests, 18 passed and two UI failures. The ambiguous
  error selector was corrected; direct QA rerun passed draft preservation,
  task completion/undo/reschedule and independent public help (2/3 tests).
- Remaining UI failure: at double text scale, the editor Save action scrolls
  to window bounds y=2523…2669 on the Xiaomi's 2670-pixel screen and is reported
  outside the visible area. Insets must be fixed and verified before acceptance.
- `:app:assembleQa :app:assembleQaAndroidTest`: successful including the new
  public-help storage/authentication failure checks, whose results are pending.
- `scripts/check-native-alignment.ps1`: all eight packaged native libraries
  passed ZIP 16 KiB offset and ELF LOAD alignment checks for the tested APK.
- `:app:writeRuntimeInventory -I scripts/runtime-inventory.init.gradle`:
  82 resolved runtime artifacts inventoried with artifact/POM hashes. Notice
  packaging and the two dependencies without named POM licences remain to finish.

No Safety screenshots were taken. This checkpoint does **not** certify the
expanded release or P2 completion. Full-screen inset repair, provider failures,
process recovery, TalkBack/contrast, background delivery and complete specification
reconciliation remain required. Owner data question from r6 remains unanswered;
no owner-package connected tests are permitted.
