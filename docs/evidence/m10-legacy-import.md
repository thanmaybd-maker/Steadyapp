# Explicit OG organiser import checkpoint

7 October 2026. Base: 33357a4. Settings/file tools now have a separately labelled OG JSON SAF picker and paginated eligible-record preview. The importer follows the actual donor exportDatabaseToJson field contract: plans, sessions, foodIdeas and reviews. It never maps, previews, hashes into identity, stores or logs the safetyNotes subtree. No owner OG database was inspected or transferred.

Input is limited to 5 MB and 5,000 eligible records, strict UTF-8, unique JSON keys, bounded nesting, exact supported fields, canonical dates and uncoerced numeric/boolean values. It refuses malformed or unsupported files before writes. Calendar dates retain boundary zero in the previewed current zone; task time slots are retained as source fields but do not invent exact appointments. Generated review text and recipe claims remain explicitly unverified. Original eligible fields are retained in typed encrypted receipts and ordinary backup, excluded from scratchpad Markdown.

Stable IDs derive from canonical eligible row fields and duplicate ordinal, independent of key order/whitespace/private content. OG supplies no source IDs: changed source rows are new records, disclosed in preview. Atomic append keeps existing records and same-date reflections. Receipts prevent reimport overwrites or recreating deleted imports, including after restart/backup restore. Reported session durations have a calendar anchor and no measured segments; UI labels them reported and shows a separate total. No habits/workouts/active alarms are invented.

Actual host command:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

```text
BUILD SUCCESSFUL in 1m 21s
117 actionable tasks: 42 executed, 75 up-to-date
Unit tests: 48; failures: 0; errors: 0
Lint blocking errors: 0; warnings: 93
```

Both isolated QA APK installations returned Success. Direct instrumentation selected LegacyOgImportTest, DocumentProviderTest, PortableRecoveryTest, HistoricalRecordsTest and FoodPlanningPersistenceTest:

```text
Time: 10.57
OK (11 tests)
```

New checks cover four-collection append, eight concurrent reimports, existing prose preservation, edit/delete/reopen, backup receipts, source Safety exclusion, absent measured intervals, a mid-import SQL refusal rolling back records and receipts, successful retry, strict UTF-8 and the smaller SAF read bound. Existing portable/storage/history regressions remain green. The previous 29-test private/storage result belongs to m10-private-notes.md, not a new full UI run.

Debug APK SHA-256: `a9a9b95a4457baf852a209ca389baa2cfc2b41dcf8a3ebbb670f6a7150acd26a`.

Remaining: unlocked SAF picker/preview/accessibility journey and daily Review navigation/optional-rating wiring. No all-parity completion, owner-data access, owner replacement or private screenshot is claimed.
