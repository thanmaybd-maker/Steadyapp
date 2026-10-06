# Steady v0.1 release checklist

Current results and remaining work: [Windows/Xiaomi checkpoint](docs/evidence/checkpoint-2026-10-06.md)
and [future plans](FUTURE_PLANS.md). The baseline build, unit/lint/device suite,
installation and launch now pass. The historical setup items below do not describe
the current toolchain; complete milestone acceptance remains pending.

Updated 6 October 2026. Earlier checked entries represented generated source, not
passing acceptance. Every milestone remains open until its evidence passes.

- [ ] M01: successful build/lint, five routes, back/insets, maximum font and TalkBack.
- [ ] M02: encrypted Room CRUD/reopen, migration schemas/tests, missing/wrong key recovery, canary inspection, backup exclusions.
- [ ] M03: persisted daily plans/minimum day/pause/shutdown, injected time, failure-preserved drafts, Food reference accessibility.
- [ ] M04: empty optional private plan and editable contacts; public help while locked/storage unavailable; verified numbers and dialer failure handling.
- [ ] M05: commit-before-schedule timer, stale callback rejection, exactly-once completion, boot/time reconciliation, cue/permission modes and actual Xiaomi matrix.
- [ ] M06: chosen week end, seven logical days with factual missingness/evidence, five indicators/four questions, failed-save handling.
- [ ] M07: credential lock/recents; exact Markdown preview and SAF; authenticated eligible-data archive and atomic validated restore; confirmed complete local deletion.
- [ ] M08: previous gates pass; versioned APK, checksum/signing identity, primary-device checks, dependency notices and final evidence.

## Build gate

- [x] Preserved imported source in local Git checkpoint `d33cce2`.
- [x] Identified Windows blocker: missing JDK and Android SDK.
- [x] Prepared checksum-verified download script and parsed its PowerShell syntax.
- [x] Corrected missing launcher icon/ProGuard file, AndroidX property, ViewModel Compose dependency, Room schema path and SDK 35 toolchain mismatch.
- [ ] Install JDK/SDK after SDK-licence agreement.
- [ ] Resolve and inspect dependency artifacts/licences/merged manifest.
- [ ] Run assembleDebug, testDebugUnitTest, lintDebug and connectedDebugAndroidTest successfully.

## Release-critical checks

- [ ] Replace plaintext backup stub with reviewed authenticated recovery.
- [ ] Ensure public help survives database/key failure at startup.
- [ ] Remove swallowed save errors, direct DAO access from UI, and personal/log payloads.
- [ ] Complete resources, labels, 56-dp targets, contrast, text reflow and non-visual alternatives.
- [ ] Verify lock, recents, export providers, malformed imports and local deletion with synthetic data.
- [ ] Read primary phone API/build via adb; record TalkBack/font/display and screen-off/Doze/battery/permission timer results.

Do not mark a release ready while critical privacy, recovery, access or primary-device
tests remain unperformed. Do not place real emergency calls during checks.
