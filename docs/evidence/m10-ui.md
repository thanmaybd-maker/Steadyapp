# OG integration batch: progress, water and record-backed widgets

7 October 2026. Parent checkpoint: `8868eb5`. This is a partial integration gate,
not completion of the 76-row merge or the expanded/P2 documentation.

Implemented concentric dashboard arcs from the configured metrics, animated water
fill from saved servings and the optional target, and habit petals from actual
quantities. Kinetic/Daybook, reduced motion, unit preferences and existing editing
controls remain available. Replaced fixed meal values and the unobserved circadian
curve with recorded meals and a planned/recorded day horizon. Removed the assumed
3-litre goal. Review text uses the theme rather than a fixed blue/grey.

Checks run against this batch:

```text
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
BUILD SUCCESSFUL in 1m 25s
117 actionable tasks: 41 executed, 76 up-to-date
Unit XML reports: 29 tests, 0 failures, 0 errors
Lint XML: 0 Error/Fatal findings (remaining warnings are not waived)

./gradlew.bat :app:assembleQaAndroidTest :app:writeRuntimeInventory -I scripts/runtime-inventory.init.gradle --console=plain
Runtime inventory: 83 resolved artifacts
BUILD SUCCESSFUL in 7s

adb -s aac76216 install -r app/build/outputs/apk/qa/app-qa.apk
Success
adb -s aac76216 install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
Success
adb -s aac76216 shell am instrument -w -r com.thanu.steady.qa.test/androidx.test.runner.AndroidJUnitRunner
Time: 38.559
OK (29 tests)
```

Device: connected Xiaomi 14, API 36. Tests use the separate QA package, synthetic
encrypted databases and injected clocks. New device cases check water save/undo
against stored records in Kinetic light and Daybook dark at text scale 2, with
reduced motion enabled. Existing editor, recovery, migration and independent
public Safety tests also pass. No private Safety screenshots were taken. These
commands did not uninstall or clear the owner's package.

Limits: the full OG audio lifecycle/portal, seven-day habit preview, focus/rest
graph, proposal scheduler, connected Gemini and remaining matrix acceptance are
still pending. These tests do not establish TalkBack order, visual pixel accuracy
or all sensor/background scenarios. Continue implementation and device validation
before any complete-release claim.
