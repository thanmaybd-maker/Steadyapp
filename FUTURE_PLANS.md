# Steady: completion plan and future feature backlog

Updated 6 October 2026. Continue the existing Gemini/GitHub checkpoint; do not
restart from Milestone 1 or replace the app. `BLUEPRINT.md` is the v0.1 acceptance
contract. This document records the next work and ideas; an idea is not a completed
feature or authorization to expand v0.1.

## Current checkpoint

- Local JDK 17 and Android SDK are installed through `scripts/build.ps1`.
- Launcher icon resources are present. MainActivity no longer opens the encrypted
  database before navigation; public Safety help has a path independent of storage.
- Encrypted portable backups, strict validated transactional restore, exact Markdown
  preview, and Android SAF destination/source pickers are wired to actual records.
- Safety plans, support contacts, keys and device settings are excluded from portable
  recovery. Destination Safety survives restore; imported timers stay stopped.
- Baseline `be3d20e` passed assembleDebug, 10 unit tests, lint (0 errors, 42 warnings)
  and 4 connected storage/recovery tests. Its APK was installed and launched on the
  Xiaomi 14, Android 16/API 36, HyperOS OS3.0.304.0.WNCINXM.
- This checkpoint adds persisted timer transitions with generation checks, completion
  deduplication, boot/clock reconciliation, contextual notification permission,
  accessible Break controls, encrypted preference storage and explicit migration
  2 to 3. Code checkpoint `a36e6da` passed assembleDebug, all 14 unit tests and
  lint (0 errors, 41 warnings). Device regression results are recorded in the
  checkpoint evidence; timer delivery and upgrade migration acceptance remain open.

## Finish v0.1 first

1. **Verification:** finish host/device regression checks for the latest changes.
   Test migration 2 to 3 preserving existing records, missing key/partial key recovery,
   timer pause/resume/stale callbacks and exactly-once completion with synthetic data.
2. **Today:** persist pause, complete shutdown, inject clock and logical-day settings,
   retain creation metadata and draft input on failed/concurrent saves. Keep Minimum
   day practical without scores, pressure or automatic deletion.
3. **Safety:** editable optional private sections and ordered contacts, reviewed/follow-up
   status, clear storage errors, safe dialer failure handling, and public help available
   while locked or storage is unavailable. Never place a test emergency call.
4. **Access and privacy:** optional device credential/biometric lock with fallback,
   recents privacy, complete local deletion even when storage cannot open, and tested
   restore/export cancellation and provider failures. Preserve private drafts on error.
5. **Break:** selectable and previewable sound/haptic/local speech, clear supported
   capability labels, persisted custom durations, and measured Xiaomi screen-off,
   idle, process recreation, denial/revocation, clock change and reboot behaviour.
   State limits for force-stop, DND, volume and OEM battery restrictions honestly.
6. **Review:** chosen week end, seven logical days with factual missingness/evidence,
   five indicators and four questions, safe loading/saving and draft preservation.
7. **Food reference:** local preference/avoid-food filtering, resource strings,
   accessible sheet and local speech where available. Avoid medical recommendations.
8. **Accessibility:** resources for all visible strings, labelled essential controls
   at least 56 dp, maximum text/display reflow, TalkBack order/focus, contrast,
   insets, dark/high-contrast and softer theme, and non-visual alternatives.
9. **Release evidence:** refresh `docs/evidence/m01.md` through `m08.md`, dependency
   permission/licence notices, merged no-INTERNET manifest, APK checksum/signing
   identity, actual phone/settings and remaining limits. Install the verified update
   without clearing existing user data. Mark complete only when criteria pass.

## Future ideas for the user's next planning session

These are candidates to choose and scope after v0.1; none is implemented here.

| Area | Candidate feature | Guardrail / acceptance question |
| --- | --- | --- |
| Planning | Reusable task templates and optional recurring routines | Can defaults help without expanding the daily workload? |
| Fast entry | Offline quick capture and app shortcuts | Does input survive interruption and remain private? |
| Home screen | Optional Today and timer widgets | Hide personal text by default; keep Safety private. |
| Timers | Named study/build presets and optional gentle break sequences | User controls cue budget; no burst of missed reminders. |
| Review | Search, date navigation and simple descriptive trends | No personality scoring, guilt or inferred health states. |
| Accessibility | User-selected text/spacing/contrast presets and local languages | Test large text, TalkBack and offline speech availability. |
| Recovery | Backup reminders, archive compatibility fixtures and recovery drills | User initiates storage; never include private Safety data. |
| Long-term use | Retention controls, history performance and schema evolution | Never silently erase records or use destructive migrations. |
| Food | Editable offline favourites and shopping notes | Avoid medical/allergy safety claims; remain optional. |
| Quality | Automated regression checks and broader Android/OEM testing | Publish evidence and limits; one phone does not prove all devices. |

## How to add more features

Record each request here with the concrete user problem, proposed screen, fields
stored, privacy implications, acceptance examples, dependencies/licences and tests.
Prioritise essential fixes before convenience features. Preserve a working Git
checkpoint before substantial changes, keep the existing five-screen structure
unless the user deliberately changes scope, and use synthetic verification data.
Internet, telemetry, medical logic, accounts, automatic contact actions, posting,
purchases and cloud sharing remain outside the current architecture.
