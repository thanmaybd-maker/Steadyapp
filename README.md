# Steady

**A private, accessible daily workspace.**

Steady helps you manage daily priorities, breaks, and personal reflection without the pressure of streaks, overdue tasks, or visual clutter. It is built entirely offline, keeps your data encrypted locally, and is optimized for accessibility.

## Build Instructions & Precise Remaining Blocker

**Current State:**
All architectural components (M01-M08), including Compose UI screens, Room/SQLCipher database setup, state machines, and AlarmManager/Notification adapters, have been implemented.

**The Blocker:**
The environment used to generate this source code hangs indefinitely during Gradle execution. Commands like `gradle wrapper` and `./gradlew assembleDebug` hang without completing their bootstrap process. Therefore, APK assembly remains unresolved and BLOCKED on this host.

**How to Build:**
1. Extract this project to a standard laptop/desktop environment.
2. Open the project in **Android Studio** (Koala or later, compatible with AGP 8.3+).
3. Let the project sync using the included `gradlew` script and `gradle-wrapper.jar`.
4. Click **Run 'app'** or execute:
   `./gradlew assembleDebug testDebugUnitTest lintDebug`
5. Install the generated APK on your Android device.

## Post-Build Verification Required
Since the sandbox could not compile the APK, please perform the following tests on your device:
- **TalkBack & Contrast Check:** Ensure touch targets are 56dp minimum and contrast is sufficient.
- **Timer & Doze Reliability:** Trigger a focus timer, turn off the screen, wait 20+ minutes, and ensure `AlarmReceiver` wakes up the device to issue the haptic/sound notification. Do not claim survival of force-stop.
- **SAF & SAF Backup:** Test the Markdown export and Encrypted portable backup destinations using the system document picker.

