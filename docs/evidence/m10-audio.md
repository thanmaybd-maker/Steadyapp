# OG audio and Focus integration checkpoint

7 October 2026. Base commit: c8b2298. This is a host-verified implementation checkpoint; device acceptance remains pending and is not a completed milestone.

The Focus route now exposes all six donor-derived procedural sounds, a real play/stop state, independent volume, a 1–40 Hz modulation dial/slider, opt-in timer linkage, encrypted preferences and three offline nature scenes with a full-screen portal. AudioTrack ownership is serialized through cleanup. Cancellation invalidates stale writers; focus loss, unplugging headphones, navigation/background and ViewModel disposal stop playback. Modulated mono sound is not labelled binaural or claimed to improve cognition. A device test writes each sound at zero volume, so it does not certify subjective audible quality.

Study sets use actual tasks, recorded sessions and saved optional effort. Scratchpad and quick thought capture use encrypted repository records. Voluntary pause offers optional 5/10/15-second practice and immediate Return/Continue; its counter and reason capture come from the same transactional event. The Focus inner ring uses actual recorded/live focus against an optional owner target.

Edited files are the audio engine/generator/preferences, Focus cards, ExpandedViewModel, interruption repository/DAO, lifecycle cleanup, resources and isolated tests. Original sound algorithms and scene assets came from the owner-supplied `steady og` inventory; its absent LICENSE is recorded in the source analysis, not replaced with an invented licence. Existing destination storage/timer/theme architecture remains authoritative.

Commands:

```powershell
$env:JAVA_HOME = Join-Path $PWD '.tools/jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $PWD '.tools/android-sdk'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

Actual final host output:

```text
BUILD SUCCESSFUL in 1m 1s
117 actionable tasks: 30 executed, 87 up-to-date
Unit tests: 32; failures: 0; errors: 0
Blocking lint issues: 0
```

Three new generator tests verify nonempty bounded output for all six sounds, deterministic continuity across buffer boundaries, silence and invalid-input rejection. New QA tests cover real PCM writes/restarts, encrypted UI preferences/restore, accessible portal controls and immediate voluntary-return persistence.

Both QA APKs installed with `adb -s aac76216 install -r` (`Success`, `Success`). The direct QA instrumentation run encountered `No compose hierarchies found`; device inspection confirmed `mWakefulness=Dozing` and keyguard `showing=true`. It was stopped against **only** `com.thanu.steady.qa`. That run did not pass. No owner uninstall, clear-data, authentication bypass, private screenshots or personal fixture reads occurred. The owner was asked to unlock the phone; rerun QA before accepting these rows.

Earlier intermediate failures are retained in local diagnostic logs: duplicate resource name and a Kotlin 1.9.22 compiler stack overflow caused by a nonlocal return inside synchronized/withLock. Those were corrected; the final host command above passed. Logs reside under ignored `.tools/merge-audit/`.

Remaining checks: unlocked-phone QA, actual listening/interruptions, graphical inspection, timer lifecycle/process-death checks and the remaining 76-row merge acceptance work. Decorative sound bars, scheduler proposals, native Review bars, additional private notes, legacy import and connected Gemini are not certified by this checkpoint.
