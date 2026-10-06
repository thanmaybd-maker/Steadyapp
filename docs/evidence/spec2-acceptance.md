# Specification 2 acceptance ledger

Updated 6 October 2026. This ledger maps implementation and evidence; it is not a
release certificate. Source documents remain unchanged. R7's workflow checkpoint
and later repairs retain the existing native app and explicit schema migrations.

| PRD contract | Present implementation and evidence | Remaining acceptance |
| --- | --- | --- |
| P-01 shell/onboarding | Five tabs, saved preferences, independent public help; authentication-cancellation check passed alone | Full tab/Back/rotation journeys; optional onboarding template; broad TalkBack |
| P-02 dashboard/plan | Task CRUD/reschedule/undo synthetic UI journey; card sizing/order; summaries and 4→5 migration; time chart/list/Add menu | Chart/shortcut/keyboard/full-shell checks; maximum system scale |
| P-03 habits | Versioned definitions/occurrences, idempotent logs, notes/undo/archive and historical correction fixtures | Every recurrence/daily-return interaction; grouped anchor presentation |
| P-04 day modes | Persisted Normal/Minimum/Pause, editable planning examples, reminder suppression | Return/no-debt/next-day device walkthrough |
| P-05 Focus | Shared elapsed-time engine, subjects/topics, queue, scratchpad, presets, actual partial time and stale callback tests | Process/window recovery, contextual cues and clock-change scenarios |
| P-06 Break/Reset | Persisted break; dim controls; skippable comfortable Reset/next step; explicit delayed-cue notice | Screen-off/Doze/reboot/force-stop; full TalkBack phase behavior |
| P-07 manual workouts | Seven modes, templates/sets/interval phases/rest/effort; encrypted workflow fixtures | Ordered exercise editing/removal, background interval phases, device delivery |
| P-08 food/water/sleep/care | Editable ideas and dated factual sources; logs/history; large-water confirmation; existing instructions/quiet hours | All edit/undo/error flows, template choices and provider checks |
| P-09 voluntary pause | Voluntary practice clearly labelled; actual returns/continues/disable recorded | Period insight display; P1 usage/interception explicitly deferred |
| P-10 Review | Week/month/calendar, linear habit history, subject totals, supporting corrections, preserved reflection fixture | All card/chart accessibility and optional review layout verification |
| P-11 Safety | Independent bundled directory; encrypted private store/drafts; corruption/public-help and isolation checks | Full-suite auth test timing repair, airplane-mode demonstration, explicit draft status |
| P-12 Settings/privacy/portability | Enforced platform auth; typed organiser allowlist; round-trip/tamper/rollback/exclusion fixtures; bounded SAF adapter | Native provider/cancellation, local deletion/relock and locked-background behavior |

P1 capabilities (steps, GPS, usage/interception, ambient audio) are deferred under
the documented feasibility gates. Manual and voluntary alternatives remain. The
app must not display inferred physiological values or claim an active platform
service that is absent.

P2 is still required by the owner's selected sequence **after expanded acceptance**:
Learn, Build, People, Money Guard, Care/debrief, Capture/integrations and optional
connected AI. Exact OpenGym/Vital3D links and AI provider selection were requested
because the specification explicitly leaves them unresolved. No paid service,
credentials, publishing, automated contact or unlicensed source reuse is inferred.

## Cross-cutting gates

- Host build/unit/lint are demonstrated for successive checkpoints; latest known
  successful run has 27 unit tests and zero lint errors (48 warnings, four info).
- Isolated QA run had 23 tests, 22 passed. The auth test sent Back before the native
  prompt existed; it passed in a standalone run and needs the repaired full rerun.
- Double-text editor controls and draft/error/task journey passed. This is not a
  blanket maximum-system-font or TalkBack acceptance claim.
- Frozen schema 4 upgraded to 5 without losing synthetic profile/water records;
  SQLCipher reported the pinned 4.9.0 core. Prior released schemas stay unchanged.
- All eight packaged native entries passed real ZIP and ELF 16 KiB alignment.
- 82 runtime artifacts inventoried; Apache/SQLCipher/OpenSSL notices are bundled
  and have an offline Settings entry. Test provider is QA-only and not a release asset.
- Owner package path remained unchanged through isolated tests. Earlier owner
  cleanup mistake/data uncertainty remains recorded in r6; do not repeat it.
- No Safety screenshots, private export fields, INTERNET permission, telemetry or
  fabricated medical/fitness measurements are accepted.

Release is open until all P0 cases and the performance/installation/recovery gates
pass. Evidence must report exact commands, commit, APK hash, test conditions and
limits. Mapping a source line to this ledger does not mark its requirement passed.
