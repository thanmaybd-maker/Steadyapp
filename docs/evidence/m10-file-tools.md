# Organiser counts and explicit preview copy/share checkpoint

7 October 2026. Base: 1105e6a. File tools show efficient SQL counts for saved organiser tasks, habits, activity sessions, food ideas, reflections, serving/habit logs, captures, ordinary notes, motion records and earlier organiser records. Scope is labelled across all dates, including archived records. Counts do not open private Safety or include credentials, drafts or machine metadata. Refresh has visible failure/retry handling.

Only a prepared scoped Markdown preview can be copied/shared through this UI. Copy uses a sensitive-marked system clipboard entry; Share opens Android's chooser with exact text and no recipients/attachments. Steady does not claim delivery. Unsupported handlers/clipboard writes and UTF-8 previews over 256 KB return visible failure while keeping the preview available for SAF file saving. No owner clipboard was read or replaced during tests, and no external app/message was launched by the checks. Adapter tests capture synthetic payloads through injected sinks.

The first count build failed because the new query incorrectly pluralized legacy table names. Existing entities confirmed weekly_review and timer_session; the query was corrected before acceptance. Draft clearing now cancels its pending loader at the clearing boundary.

Actual host command:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

```text
BUILD SUCCESSFUL in 1m 20s
117 actionable tasks: 30 executed, 87 up-to-date
Unit tests: 49; failures: 0; errors: 0
Lint blocking errors: 0; warnings: 90
```

Both isolated QA APK installations succeeded. Direct instrumentation selected MarkdownSharingTest, OrganiserRecordCountsTest, ReviewDraftPersistenceTest and DocumentProviderTest:

```text
Time: 2.142
OK (8 tests)
```

New checks verify historical/archived count scope, private-store independence, internal-note exclusions, exact Unicode clipboard payload/sensitive flag, recipient-free chooser construction, failure signalling and byte bounds without truncation or side effects. Relevant encrypted draft and SAF regressions remain green.

Debug APK SHA-256: `5b907456201bc3745b76a3a9a454c9e91890a8cf27adc2e503e5875a3c974258`.

Android APIs verified against primary documentation: [text sharing](https://developer.android.com/develop/ui/compose/sharing/send), [clipboard](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste). No new dependency or permission was introduced.

Remaining: unlocked Settings/clipboard/chooser accessibility acceptance and the other coverage rows. Whole-project completion remains unclaimed.
