# Private notes, masking and public grounding checkpoint

7 October 2026. Base: 2190052. Additional Safety notes now have category, create/edit/delete and pin controls in the independently encrypted private database. Explicit schema 2-to-3 migration preserves existing structured plans/contacts/drafts; frozen schema fixtures remain available. Note drafts persist before save, survive restart and remain after refused writes. Revision checks prevent stale overwrites. Ordinary organiser export/backup/restore never queries or includes these notes.

Safety starts masked and remasks on background. Masked private text is removed from composition/accessibility. Existing authentication and secure-window protections remain. Public sensory grounding offers optional/skippable steps, exit and public help without a private-store dependency. Trusted-contact SMS uses an explicit empty composer, with handler failure visible; no message or call is sent automatically.

Actual command:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

```text
BUILD SUCCESSFUL in 1m 24s
117 actionable tasks: 48 executed, 69 up-to-date
Unit tests: 45; failures: 0; errors: 0
Lint blocking errors: 0; warnings: 90
```

Both isolated QA APK installations succeeded. Direct instrumentation selected PrivateNotesPersistenceTest, ContactIntentTest, FoodPlanningPersistenceTest, RoutineSchedulingTest, StudyBlockPersistenceTest, AlarmCuePersistenceTest, ExpandedFoundationTest, CompletionWorkflowTest, PortableRecoveryTest, HistoricalRecordsTest, EncryptedRecoveryTest and DocumentProviderTest:

```text
Time: 25.635
OK (29 tests)
```

Checks cover private schema 1/2-to-3 upgrades, encrypted reopen, note pin/revision/delete/drafts, refused-save draft preservation, exclusion from ordinary files and synthetic SMS intent/handler failure. Existing storage/platform regressions remain green. Assertions avoid printing private fixture values. No private screenshots, owner-data reads, owner-data replacement or live SMS/call occurred.

```text
PASS: all 8 native entries have 16 KiB ZIP and ELF LOAD alignment.
```

Debug APK SHA-256: `c89a17bbb6186bec4fa72a8238b8fa42e7a4931ec394113b00f77ffc864cc9a9`.

Remaining: unlocked grounding/masking/large-text interaction acceptance. UI journeys are compiled, not accepted while keyguard remains showing. Private data must not be captured during UI verification. Legacy OG organiser import, connected provider and the remaining coverage rows are still pending; this checkpoint does not claim whole-project completion.
