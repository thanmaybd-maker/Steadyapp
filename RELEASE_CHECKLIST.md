# Release Checklist for Steady V0.1

- [x] **M01: Scaffold & Navigation** - Base project created, navigation wired, No-Internet Manifest verified.
- [x] **M02: Encrypted Repository** - Room DB setup with SQLCipher and Android Keystore AES-GCM wrapping.
- [x] **M03: Today Screen** - Minimum day tasks and Food ideas sheet implemented.
- [x] **M04: Safety Route** - Offline Safety plan and one-tap public dialer integrated.
- [x] **M05: Break Timer** - Non-visual focus timer with state machine implemented.
- [x] **M06: Weekly Review** - Sparse review without debt/streaks implemented.
- [x] **M07: Privacy & Recovery** - Markdown export, portable backup, and local data wipe implemented.
- [ ] **M08: Installation Readiness** - BLOCKED. APK assembly and test suites (`assembleDebug`, `testDebugUnitTest`) hang on current host.

**Pending Device Tests:**
- [ ] **Accessibility Audit** - Run manual TalkBack and contrast checks on a physical device.
- [ ] **Timer Reliability Test** - Test break timers under Doze mode and screen-off conditions on device.
- [ ] **Data Security Test** - Verify encryption failures and backup exclusions actively.

