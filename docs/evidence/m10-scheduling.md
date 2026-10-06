# OG study blocks, reminder privacy and Usage Access checkpoint

7 October 2026. Source base: 317959a. This checkpoint records verified implementation, not completion of the whole merge.

Today now offers three editable study-block templates with real time/duration/category/primer inputs, conflict counts and transactional adoption. Opaque proposal IDs and deterministic linked plan IDs survive renaming, repeated adoption, restart and backup. Revision checks reject stale edits. Deleted associated plans are not silently recreated. Settings and associations use encrypted organiser storage; they are not ordinary Markdown scratchpad notes.

Per-block/global reminders, lead time, permission request/state and a clearly labelled test notification are wired. Reminder scheduling reads the adopted plan, respects quiet hours, Pause/Minimum, the daily alert budget and stale grace. Reminder taps target Today or Focus after the existing access gate; they do not start activities automatically.

Background/locked activity and routine receivers can deliver only generic cues from a bounded, non-personal operational token cache. They do not open personal Room/Keystore stores while inaccessible. Tokens include opaque UUIDs, generation/boot/deadline/expiry and cue flags, without titles, Safety, health or notes. Atomic claims prevent duplicate delivery. Deferred routine receipts reconcile into private history after access; acknowledgement removes only the captured receipts. Boot/time changes invalidate scheduled tokens; imported live alarms are never resumed. Native alarms cannot survive Android force-stop; reboot reconciliation marks interrupted timers rather than inventing progress. This cache is excluded from backup/export and OS backup.

Usage Access is reachable in Settings, requests the OS special access and reads bounded event windows on demand. It clips intervals, handles repeated transitions and current foreground intervals, and refreshes on return. Denial, revocation or unavailable events show honest failure/permission states. Usage summaries do not claim cross-app interception or time saved, and are not persisted/exported/sent.

Exact host command (local JDK 17/SDK):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

Actual output:

```text
BUILD SUCCESSFUL in 1m 26s
117 actionable tasks: 52 executed, 65 up-to-date
Unit tests: 42; failures: 0; errors: 0
Lint blocking errors: 0; warnings: 90
```

Installed only the isolated QA app/test APK with `adb -s aac76216 install -r`. Both returned `Success`. Direct QA command:

```powershell
adb -s aac76216 shell am instrument -w -r -e class com.thanu.steady.StudyBlockPersistenceTest,com.thanu.steady.AlarmCuePersistenceTest,com.thanu.steady.ExpandedFoundationTest,com.thanu.steady.CompletionWorkflowTest,com.thanu.steady.PortableRecoveryTest,com.thanu.steady.HistoricalRecordsTest,com.thanu.steady.EncryptedRecoveryTest,com.thanu.steady.DocumentProviderTest com.thanu.steady.qa.test/androidx.test.runner.AndroidJUnitRunner
```

```text
Time: 21.958
OK (23 tests)
```

These use synthetic data only and cover encrypted migrations, organiser backup/rollback, historical edits, repeated adoption/rename/restart/delete, metadata exclusion/validation, atomic locked cue claims and receipt races. They do not certify notification delivery under every OEM power policy or a fully exercised user interface. No owner data was deleted, opened or captured.

Delivery APK SHA-256: `dd64cf5954f724483c88075f004e19783766b92736dc084051b5181db096dbce`.
`.\scripts\check-native-alignment.ps1` returned `PASS: all 8 native entries have 16 KiB ZIP and ELF LOAD alignment.`

Device: Xiaomi 14, API 36 / Android 16 build BP2A.250605.031.A3, HyperOS 3.0.304.0.WNCINXM. Keyguard still reports showing/Dozing. Unlocked screen/audio journeys and notification permission grant/deny/revoke, system cancellation/tap/quiet-hour scenarios remain pending. The owner APK has not been updated with this checkpoint.

Next: finish and verify scheduler platform/UI edge cases, then food adoption, additional private Safety notes/grounding, strict legacy organiser import and isolated Gemini BYOK. The 76-row [coverage CSV](m10-steady-og-ledger.csv) records remaining work; no pending row is claimed complete.
