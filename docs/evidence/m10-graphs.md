# OG record-backed graphs checkpoint

7 October 2026. Base: 7a9d6d3. Host checks pass; unlocked-device acceptance and visual inspection are pending.

Implemented OG paired daily Focus/Rest bars in native Compose with minutes, exact totals, nullable empty ratio, selectable dates, labelled alternatives and selected-day supporting records. Edits/deletions use existing history repositories. No CDN, inferred rest percentage, sample history or physiological scoring is used. Actual closed segments retain historical timezone/boundary policy; overlapping segments of the same kind are unioned.

Today includes seven-day habit circles and full date/state text from historical definitions and actual occurrences. Optional neutral current/longest runs explicitly apply only to that seven-day range. Skipped/not-due/Pause/Minimum-suppressed dates are excluded; missing/partial past days end a run; today's unfinished occurrence remains open. These summaries create no new activity records.

The day horizon uses the injected clock for a minute-updated current-time marker, restricted to the selected logical-day bounds. Reduced motion removes pulsing. Focus shows paused state rather than a misleading NOW badge. Audio decoration is explicitly labelled as decorative, and source/scene provenance is accessible in Settings.

Actual command:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

Actual output:

```text
BUILD SUCCESSFUL in 1m 5s
117 actionable tasks: 48 executed, 69 up-to-date
Unit tests: 35; failures: 0; errors: 0
Blocking lint issues: 0
Donor inventory: 62 files; mismatches: 0
```

New deterministic tests cover actual focus/rest with overlapping segments, absence of an empty ratio, stored boundary/DST splits, neutral consistency semantics and bounded marker position. New isolated QA journeys are compiled for day selection/correction/deletion, 2x text, seven-day states and a synthetic chart-only image. They have **not run successfully yet** because the Xiaomi still reports Dozing/keyguard showing. The owner app and private Safety are not captured or read.

Next: run the QA journeys after unlock, inspect the synthetic graph image, continue scheduler/food/private-note/legacy-import/connected-provider gaps and update each row only with applicable evidence. This checkpoint does not certify those remaining features.
