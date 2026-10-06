# OG food choices and recipe-to-plan checkpoint

7 October 2026. Base: 7711149. Food recipes now retain optional meal type/context in encrypted organiser metadata, with transactional saving alongside the existing recipe. The editor offers 5/10/15/25-minute prep choices, selectable pantry items and editable custom ingredients. Existing budget, vegetarian preference, avoidance tags, favorites and explicit consumption logging remain in use.

Health exposes an editable scheduling sheet for a selected saved recipe: title, date, optional time, planned duration and notes. Explicit adoption writes one Food plan and a stable adoption association atomically. Repeated adoption, renaming or deleted associated tasks cannot silently create another plan. Planned Food tasks do not start focus automatically or create a meal/workout record. The existing task editor can reschedule/edit them.

The implementation uses existing encrypted tables; no schema reset or destructive migration was added. Typed recipe context/adoption metadata round-trips in organiser backup and is validated before import. It is excluded from ordinary scratchpad Markdown. Deleting a recipe removes its context and preserves existing task snapshots/adoption history. Private Safety, contacts, credentials and live cues are not queried or added to file scope.

Actual host command:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

```text
BUILD SUCCESSFUL in 1m 18s
117 actionable tasks: 46 executed, 71 up-to-date
Unit tests: 44; failures: 0; errors: 0
Lint blocking errors: 0; warnings: 90
```

Both isolated QA APK updates returned Success. Direct QA regression selected FoodPlanningPersistenceTest, RoutineSchedulingTest, StudyBlockPersistenceTest, AlarmCuePersistenceTest, ExpandedFoundationTest, CompletionWorkflowTest, PortableRecoveryTest, HistoricalRecordsTest, EncryptedRecoveryTest and DocumentProviderTest:

```text
Time: 22.808
OK (26 tests)
```

The new encrypted test verifies context/recipe restart, invalid context refusal, 12 concurrent repeated adoptions producing one Food plan, no invented meal/activity, backup/restore, rename/reschedule/delete and deleted recipe source history. Two new unit checks cover pantry/custom retention and validated stable adoption identities. The Daybook Dark 2x Food journey is compiled but has not passed on-device: keyguard remains showing. No private Safety images/logs or owner data were collected, cleared or overwritten.

Remaining: unlocked UI/draft/accessibility acceptance, richer mini-summary cards and the connected search/suggestion provider. Connected recipe generation is not claimed by the manual food workflow. Full parity remains pending as recorded in the [coverage CSV](m10-steady-og-ledger.csv).
APK SHA-256: `32eadd4598e7778c2a2ee9ce1985a2822d4d7c1ef3049720d273c3149fbc0efa`.
