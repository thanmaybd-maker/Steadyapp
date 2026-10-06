# Steady completion plan

Updated 6 October 2026. The owner selected **expanded release first, then P2**.
Specification 2.0 in `Steady_Documentation/` supersedes the earlier five-screen v0.1
scope. Continue the existing native project and preserve its records and Git history.

## Working checkpoints

- `c498d4c`: original local delivery baseline, pushed to the authoritative repository.
- `ecfe758`: supplied specification pack preserved unchanged.
- `26c5cea`: encrypted expanded data model, explicit migration 3 to 4 and independent Safety store.
- `940394c`: five expanded tabs, onboarding, task/habit editors, actual-time activity controls,
  health records, period review and platform authentication. Build/unit/lint passed; phone install
  and launch succeeded without clearing data.
- `e082f01`: organiser-only typed portable recovery, scope preview, date/category Markdown,
  Safety contact editor and historical-zone corrections. Host build/unit/lint/test packaging passed.
  Acceptance evidence lives in `docs/evidence/`; code existing is not the same as every criterion passing.
- `862b8d1`: interval/rest/template workflows, encrypted private drafts, reminders,
  editable care/routines, reviewed food references and draft/export corrections.
- Subsequent personalization/inset work: three selectable summaries, water shortcuts,
  Review visibility, time chart/list and persistent Add; explicit organiser migration
  4→5; offline notices and QA-only provider. Host checks passed 27 unit tests with
  zero lint errors; all 27 isolated device checks passed in 40.605 seconds.

## Complete expanded P0 acceptance

1. Verify the implemented routine/card/subject/topic/interval/strength/rest workflows across
   all essential journeys. Finish optional onboarding templates and ordered exercise controls.
2. Verify quiet hours/budget/Pause/no-catch-up on-device. Finish the documented locked-background
   cue contract and contextual cue handling. Pocket Reset next-action editing is implemented.
3. Verify editable bundled food/reference and capability states, alongside shortcut/log corrections.
4. Complete draft/process recreation, authentication cancellation, storage/key failures, local deletion,
   provider cancellation/failure and migration scenarios. The 27-test isolated QA suite now passes;
   broader real-provider/process/window scenarios remain. Safety stays structurally excluded.
5. Verify exact totals, history corrections, archived habits, note export and interrupted restore.
6. Run synthetic Compose/device accessibility checks, maximum text, TalkBack, theme contrast,
   screen-off/Doze/denial/revocation/clock/reboot scenarios and ten-year data/performance measurements.
7. Finish release identity, no-INTERNET manifest, clean build,
   artifact hash and install/update evidence. Reconcile every source point before declaring P0 complete.
   Notices/82-artifact inventory and actual ZIP/ELF 16 KiB alignment are implemented/checked.

Use `docs/evidence/spec2-acceptance.md` for the remaining acceptance ledger. Source indexing
keeps all 527 points and distinguishes mapped requirements from demonstrated acceptance.

## P1 capability gates

Phone steps, active GPS, Usage Access, optional app interception and local ambient audio
ship only after their own device, permission, lifecycle and licence gates pass. Unsupported
capabilities must be plainly unavailable or deferred; they must never display simulated measurements.
No sensor or location service is enabled in the current checkpoint. Manual logging remains available.

## P2 after expanded release

- Learn: retrieval attempts, correction, topic states, spaced review and optional exam-answer structure.
- Build: six-line project contracts, acceptance gates, freeze/rehearsal/fallback and reviewable agent briefs.
- People: private callbacks and observation/request notes, with no buddy network or automatic messages.
- Money Guard: optional discretionary caps, renewals and a user-controlled purchase waiting period.
- Care/debrief: factual user records and explicit scoped summaries, without medical inference.
- Capture: share target, then separately permissioned voice/OCR and integration conflict rules.
- Connected AI: a separate variant and data-flow contract; exact selected-field preview, editable proposals,
  explicit apply, cancellation and quotas. Safety/contacts/keys never enter prompts. No provider credentials
  or valid connected service have been supplied.
- OpenGym/Vital3D: obtain exact source links and verify licences before any source or asset reuse.
- Deeper Obsidian/calendar/health integrations: establish ownership, permissions, deduplication and conflicts.

For each feature, record the user task, fields, export policy, accessibility/error states and acceptance
evidence. Use synthetic fixtures; retain working checkpoints. No task is complete solely because it compiles.

