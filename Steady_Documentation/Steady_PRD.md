# Steady — Product Requirements Document

**Specification:** 2.0 · **Date:** 6 October 2026 · **Owner:** personal Steady project · **Status:** proposed implementation contract.

Read with `Steady_Tech_Stack.md` and `Steady_Design_Document.md`. This PRD reconciles the two uploaded application codebases, seven Stitch design exports, earlier architecture, supplied briefs and conversation. It describes the intended product; it does not certify a working APK. Source IDs below match the technical document. Every archived file is accounted for in `Source_Inventory.csv`.

## 1. Product definition

Steady is a private, customizable Android companion for daily planning, habits, study, project work, movement and reflection. It helps its owner choose a manageable next action, record what actually happened and adjust the next day. It is designed for continued daily use over years, with no three-week expiry or compulsory program to finish.

The interface combines the Stitch dashboard's visual richness with the Expo prototype's calm daybook language. Health and Focus become full destinations. Safety remains immediately accessible, even when the personal database cannot open. The first expanded release works offline and without an account.

The central problem is fragmentation: a student and builder must coordinate coursework, projects, personal routines, movement, notes and recovery across disconnected tools. Adding more tracking can itself become work. Steady should make ordinary days easier and difficult days smaller, while showing honest records instead of a simulated ideal life.

## 2. User needs and constraints

The primary user is the project owner, an Android user in Karnataka who studies and builds software, prefers practical learning and wants configurable support for both. The conversation identifies significant visual-access needs and variable energy. These are design inputs, not a psychological diagnosis. The app must not embed a biography, private addresses, family details, relationship history or medical incident narrative from this conversation.

| Need | Product response | Validation |
|---|---|---|
| Read and operate the app comfortably | Large controls, scalable text, high contrast, TalkBack, dim timer view | Device accessibility sessions using realistic content |
| Turn ambitious plans into actions | One next action, task queue, optional time blocks and evidence notes | Start an intended task without navigating multiple modules |
| Continue on low-capacity days | Minimum Day and Pause Day; no backlog punishment | Switching modes preserves existing records and reduces visible demands |
| Capture effort accurately | Actual focus/workout duration, editable logs, data-source labels | Reconcile displayed totals against stored records |
| Learn and build without endless re-planning | Study subjects, project tags, scratchpad; deeper Learn/Build tools staged later | Complete and record one bounded work session |
| Own personal information | Offline core, encrypted storage, explicit portable exports | Privacy and restore acceptance suite |
| Work from a phone-centered development setup | Downloadable build artifact from a proven host | Clean-checkout build and installation on the primary phone |
| Use fitness tools without buying a wearable | Manual workouts and supported phone observations | Capability checks and real-device trials; manual fallback always works |

The proposed primary test device is the Xiaomi 14 referenced in the supplied planning material. Confirm its actual Android/HyperOS version at implementation time. Do not hardcode capabilities based on the model name.

## 3. Goals, boundaries and release structure

**Product goals:** make the next action clear; lower the effort of logging; support sustained use without guilt; preserve privacy and history; make every displayed measurement explainable; keep accessibility available throughout the app.

**Excluded from the core:** diagnoses, symptom interpretation, supplement dosing, automated exercise prescriptions, social feeds, buddy accountability, competitive rankings, advertising, compulsory cloud accounts, fabricated sensor data, and promises of dependable cross-app blocking before device evidence exists. Existing care instructions may be recorded by the user, without the app rewriting them.

This PRD defines an expanded personal-use release, called **Steady Expanded** here. “Specification 2.0” is a documentation revision, not an APK version or claim that the earlier v0.1 shipped. Choose the package version after examining the actual repository and installed app.

| Priority | Meaning | Scope |
|---|---|---|
| P0 | Required for the expanded personal-use release | Offline shell; private storage; Safety; Today and habits; Focus; manual workouts; food/water/sleep logs; Review; export/recovery; accessibility |
| P1 | Include only after its platform gate passes | Phone steps, active GPS workouts, usage insights, optional interruption, local ambient audio |
| P2 | Retained backlog with separate acceptance criteria | Full Learn/Build modules, People, Money Guard, voice/OCR, connected AI and richer integrations |

A P1 failure must result in a clear unavailable or deferred feature, never an active-looking mock. The P0 release can ship without interception or GPS if its scope explicitly says so.

## 4. Primary journeys

**First use:** choose a display name or skip; confirm country/timezone/day boundary; choose appearance and accessibility; enable useful modules; create one routine or task. All permission requests occur when their feature is used. No medical profile is required. Public Safety is usable before onboarding ends.

**An ordinary study day:** open Today, choose a task, start a subject-linked Focus session, make a scratchpad note, pause or finish, and save the actual time. The session appears in Today and Review once. A late completion notification cannot create a duplicate record.

**A lower-energy day:** switch to Minimum Day, retain only selected essentials and optionally take a Pocket Reset. Earlier completed work stays in history. Returning to Normal does not create overdue debt. Pause Day is also available without explaining why.

**A workout:** choose an activity, see which data can actually be recorded, start, pause if needed, and finish. Strength uses manual sets/reps/weight. An outdoor route requires explicit location permission and visible active tracking. Editing or deleting the workout updates relevant summaries.

**An interrupted intention:** choose a voluntary pause, or use a proven optional interception mode. The pause offers Return to focus, temporary continuation and disable controls. Essential calling, authentication, navigation and Safety remain accessible. The event record describes what occurred, without inventing minutes saved.

**Weekly adjustment:** inspect week/month history, identify missing versus skipped records, correct a mistake, and write one next-week adjustment. Computed totals may change when history changes; the user's written reflection must remain intact.

**Recovery:** export selected records as readable Markdown or create an encrypted eligible-data backup. Preview a restore before committing it. A failed restore leaves existing records intact. Private Safety content is never included in these ordinary files and is not replaced by restoring an organiser backup.

## 5. Functional requirements and acceptance

### P-01 — Navigation, onboarding and personalization · P0

Provide Today, Health, Focus, Review and Settings tabs. Preserve each tab's navigation state. Expose a consistently placed labeled Safety action from all primary screens and timer/workout views. Store the user's selected name, modules, palette, layout and date preferences; do not ship the prototypes' generic personal identity as real data.

**Accept when:** all five tabs open; Safety is reachable without completing a task or stopping a timer; Back behaves predictably; restart preserves selected preferences; fresh install shows empty or instructional states, with no invented completed activity or medical details. Source: E, N, S0–S6, U3.

### P-02 — Today dashboard and plan · P0

Show date, optional greeting, three configurable progress summaries, next action, routine anchors, task timeline and quick actions. Default rings are steps, focus minutes and habits, but unavailable steps must say unavailable or invite setup. Allow supported cards to be hidden, reordered and resized within accessible layouts.

Tasks include title, category, optional subject/project, planned duration, optional time slot, priority and completion state. Adding or editing a task never requires AI. A timeline represents plans, recorded events or explicitly self-reported energy, not measured hormones or readiness.

**Accept when:** adding, completing, undoing and rescheduling persist after restart; dashboard changes persist; no-target and unavailable rings remain understandable; aggregate values match underlying records; large text does not hide primary controls. Source: E routines, N PlanItem, S3, U2/U3.

### P-03 — Habits and routine history · P0

Support checkbox, count, duration and quantity habits; editable units and targets; selected weekdays or simple repeat schedules; optional notes and reminders. Each scheduled occurrence has its own pending, partial, completed, skipped or missing state. A habit definition is not a daily completion record.

Store historical title/schedule/target snapshots. Changing a habit today must not rewrite last month's meaning. Archive a habit without deleting its history. Offer undo for accidental logging. Use completion summaries without compulsory streak rewards or punitive broken-streak messages.

**Accept when:** edits affect the intended effective date; a non-scheduled date is not counted as a failure; duplicate taps do not double-log; archived habits remain in historical Review and export; a new logical day starts with the appropriate occurrences. Source: E/N, S3/S6, Blossom and BuddyHabit pattern references in U3; buddy features excluded.

### P-04 — Normal, Minimum and Pause Day · P0

Normal shows the user's enabled plan. Minimum shows selected essentials and hides optional demands without deleting them. Suggested editable anchors may be 10 minutes of study, 10 minutes of building and 5 minutes of comfortable movement, with movement entirely optional. These are planning examples, not exercise instructions or quotas. Pause suppresses optional goals and reminders while retaining Safety and existing history.

**Accept when:** transitions work in either direction; a day-mode change never fabricates completion; previous work remains; paused or unscheduled items do not inflate failure counts; the next day follows the chosen policy. Source: A/B, E, conversation.

### P-05 — Focus, subjects, task queue and notes · P0

Offer Study and Build session labels, subjects/topics, a small task queue, plain-text/Markdown scratchpad and presets 15/3, 25/5, 50/10 and 90/20 minutes, plus custom and open-ended sessions. These are editable presets. Pause, Resume, Stop and Save partial must be explicit. Record actual elapsed active time separately from planned time and break duration.

One active Focus session is allowed. Starting a workout while Focus is active asks whether to pause or end Focus; passive step observations may continue. Notes autosave with honest pending/saved/error status. Optional reflection records what was completed and perceived difficulty; never populate a rating on the user's behalf.

**Accept when:** skipping after two minutes of a 25-minute session records the actual two minutes, not 25; paused time is excluded; duplicate callbacks produce one result; rotation/process recreation recovers state; past-session editing updates totals while retaining notes; export preserves note content. Source: E Break, N Break/StudySession, S2/S5, U3.

### P-06 — Breaks, dim mode and Pocket Reset · P0; audio · P1

Provide configurable breaks, optional sound/haptics, readable dim mode and Pocket Reset: Pause, Breathe comfortably, Choose one next step. Every step can be skipped. Keep Stop, Exit and Safety visible and screen-reader accessible. Restore window brightness on exit. Ambient audio, if included, uses locally bundled licensed files and independent volume controls.

**Accept when:** break state survives navigation; notification denial produces clear limitations; no every-second TalkBack announcements; alarms never silently promise exact delivery when unavailable; normal process death, reboot and force-stop cases are documented separately; essential navigation remains usable in dim mode. Source: A, E, N, S2/S5.

### P-07 — Workouts and movement · P0 manual; P1 sensors/routes

Provide Walking, Running, Cycling, Strength, Intervals, Mobility and Custom modes. Record start/end, pause segments, duration, notes and optional effort. Strength includes exercise templates, sets, repetitions, weight and rest timers. Intervals include configurable work/rest rounds. Manual logging remains available when sensors or permissions are absent.

Where enabled, distinguish phone-observed steps, active GPS distance/routes, user-entered values and calculated estimates. Record provenance and observation time. Do not infer steps from workout minutes or label fixed formulas as Xiaomi sensor algorithms. Do not infer heart rate, posture, sweat loss or physiological readiness from unsupported inputs.

Optional activity-calorie estimates require the necessary user input and an explained method; no assumed 70 kg body mass or artificial minimum calories. Keep estimates separate from direct measurements and prevent overlapping step/workout estimates from being added twice.

**Accept when:** zero-length/canceled activity does not create artificial exercise; editing/deleting recalculates affected summaries; denied permission supports manual mode; stopping ends active tracking; GPS interruption displays missing coverage rather than a fabricated straight-line workout; screen-off behavior is tested on the actual phone. Source: N Health, S0/S4, U3.

### P-08 — Food, water, sleep and existing care routines · P0

Food supports editable meal ideas, favorites, ingredients, vegetarian/mixed preferences, budget, preparation time and user-entered avoidance tags. Preserve the earlier request for practical fruit/food ideas and nutrient-source reference content, with a content-review date and no treatment promises. Recipes and defaults remain editable. Ingredient filtering must not claim to guarantee allergy safety.

Water logs use actual serving amounts with custom sizes and an optional target. Provide undo and history correction. No mandatory universal intake or silent 5 L cap. Sleep is manually entered with bedtime/wake time and optional restedness; do not infer sleep stages. Care reminders reproduce instructions entered by the user; the app does not choose supplements, diagnose deficiencies or calculate doses.

**Accept when:** overnight sleep is represented correctly; water totals equal logs; recipes can be edited/favorited/deleted; empty targets are valid; no physiological score appears without a defined valid source. Source: A/B, E food reference, N FoodIdea/telemetry, U2/U3.

### P-09 — Distraction pause and usage insights · P0 voluntary; P1 platform integration

The core offers voluntary focus intentions and a practice pause screen. Optional Usage Access enables observed usage summaries; it does not itself block apps. Cross-app interception is a separate feasibility-gated capability, with clear enable/disable controls, essential-app exemptions, bypass, permission state and platform limitations.

Capture only the minimum required event/app metadata. Do not read screen contents, messages or credentials. Do not use anti-uninstall behavior or prevent leaving the app. Permission revocation cannot break planning or Safety.

**Accept when:** voluntary practice is labeled; Active appears only with a functioning supporting service; blocked/returned/bypassed counts come from recorded events; unsupported interception is absent or marked unavailable; Xiaomi demonstration includes bypass and revoked-permission cases. Source: S1, N Regain mock, U3.

### P-10 — Review and adjustments · P0

Provide week/month navigation; focus by subject; habit matrix; workout and optional steps/water/sleep history; and short optional reflection. Show differences with clear units and denominators. Missing information, skipped items and genuine zero values remain distinct. Any chart has a readable list/text alternative.

Reflection prompts include what helped, what was too demanding, what evidence changed, and one adjustment. Optional highlight, obstacle and tomorrow priority preserve useful native prototype ideas. No invented population percentiles, statistical confidence, clinical interpretation or causal claims from sparse personal logs.

**Accept when:** controlled fixture records produce exact expected totals; historical edits update charts without wiping written reflections; date windows respect time policy; sparse and empty periods are usable; a chart datum opens its supporting records. Source: E weeklyReflection, N DailyReview, S6.

### P-11 — Public and private Safety · P0

Public help uses a bundled, region-labeled, reviewed directory, separate from personal storage. India may be suggested during onboarding, with explicit confirmation. Public dial actions open the system dialer; any message action opens a preview/composer and does not send automatically.

Private Safety starts empty and may hold warning signs, coping steps, support contacts and user-entered care information. It is stored separately and encrypted. No inferred diagnosis or seeded medical profile. An app lock or private-data failure must not hide the public help route.

**Accept when:** airplane mode, organiser corruption, private-store corruption and authentication cancellation still permit public Safety access; numbers are checked against official sources before release; private content does not enter ordinary exports, backups, diagnostics or connected prompts. Source: A/B, E/N Safety, conversation.

### P-12 — Settings, privacy, export and restore · P0

Expose appearance, accessibility, enabled modules, day policy, reminders, permissions, lock, export/recovery and diagnostics. Authenticate through an enforced platform mechanism; a visible PIN toggle without real protection is unacceptable. Personal records reside in encrypted stores; non-sensitive bootstrap settings are narrowly separated.

Markdown export includes a selected date range/categories with preview and a clear readable-file warning. Portable backup encrypts eligible organiser records with a user passphrase. Safety, support contacts, keys and credentials are always excluded. GPS geometry is excluded by default and needs explicit scope selection. Restore validates and previews before an atomic commit; interrupted timers are not restarted automatically.

**Accept when:** wrong passphrase/corrupt/oversized backup does not alter existing records; complete eligible-data round-trip preserves relationships and history; Safety remains unchanged; file-provider cancellation and disk-full errors never report success; deleting local data explains the limits for already-exported files. Source: A/B, E export, N export/restore defects.

## 6. Nonfunctional requirements

| ID | Requirement | Release evidence |
|---|---|---|
| Q-01 | Offline core works without INTERNET permission, login or analytics | Merged manifest inspection and offline end-to-end journey |
| Q-02 | Personal storage encrypted; no destructive schema migration | On-device storage inspection, migration and key-failure tests |
| Q-03 | App remains usable with notifications, activity or location denied | Permission-denial and revocation matrix |
| Q-04 | 56 dp primary touch targets; scalable text; TalkBack; reduced motion | Manual checks, automated scans where applicable, measured contrast |
| Q-05 | Essential normal text aims for 7:1; other normal text at least 4.5:1; large text/non-text at least 3:1 | Actual rendered color-pair measurements in every supported theme |
| Q-06 | Years of records do not require loading all history to show Today | Synthetic decade dataset, bounded queries, backup/restore benchmark |
| Q-07 | Proposed targets: warm Today ≤2 s, visible response ≤100 ms, normal save ≤500 ms; smooth 60 Hz rendering | Measured on agreed reference device; report p95, workload and exceptions |
| Q-08 | No active location/sensor work after a stopped session | Service lifecycle and battery observations, including screen-off use |
| Q-09 | Historical time interpretation stable | Boundary, timezone, clock change, reboot and daylight-saving fixtures |
| Q-10 | Recoverable errors preserve existing records | Corruption, migration and interrupted-import scenarios |
| Q-11 | Diagnostics contain technical events, not personal content | Log review and export inspection |
| Q-12 | Licensed local assets; reproducible build identity | Asset/dependency inventory, clean build, signing and artifact metadata |

Performance figures are acceptance targets to benchmark, not prototype measurements. If a target needs revision, document the measured reason before changing it.

## 7. Time, truth and privacy rules

The proposed initial timezone is Asia/Kolkata with a configurable 04:00 logical-day boundary. Confirm during setup. Preserve historical policy snapshots; changing the boundary should not reinterpret all old dates. Imported legacy records with calendar-midnight dates retain that provenance. Split actual activity across day boundaries consistently rather than giving an entire long session to whichever screen is open.

Every value must be one of: user entry, phone observation, imported observation, derived aggregate or estimate. Unknown is not zero. A planned task is not completed work. A background API being present is not proof an event happened. An attractive graph is not evidence of physiology.

No private content is prefilled from this conversation. The target stores only what the user deliberately enters or enables. Any later connected feature requires a preview of the exact outbound fields, explicit consent and a separate privacy contract. Safety content is excluded. Do not describe the offline core and an AI-connected variant with the same unconditional “never leaves your device” claim.

## 8. Source coverage and retained backlog

| Source capability | Decision | Destination / timing |
|---|---|---|
| E paper palette, orbit identity, routine anchors | Reuse original ideas after asset review | Daybook option, Today; P0 |
| E Pocket Reset and gentle reflection | Adapt | Focus/Safety and Review; P0 |
| E basic tests and file workflows | Retain relevant cases/flows, replace storage assumptions | P0 verification and export |
| E server/OAuth/MySQL/tRPC template | Exclude from native core | No matching product need |
| N native cards, recipes, workout modes | Adapt after correcting data and navigation | Today/Health; P0/P1 |
| N AI task breakdown, meal ideas, reflections | Retain as optional proposal flows | P2 connected variant; structured preview/apply |
| N simulated telemetry, seeded health data, fake PIN | Replace, never ship as genuine features | P0 corrections |
| S0 Health / S4 workouts | Reuse structure and controls, remove unsupported readings | Health |
| S1 interruption | Reuse layout with bypass and honest capability state | Voluntary P0; integration P1 |
| S2 detailed / S5 compact Focus | Merge queue/notes with readable central timer | Focus |
| S3 Today | Primary dashboard reference | Today |
| S6 Review | Reuse matrix and summary layout; remove invented statistics | Review |
| Original Today, Focus, Body, Review, Safety, portability | Retain and expand | P0/P1 described above |
| Learn: retrieval cycle, topic states, spaced review, exam answer structure | Retain without forcing daily curriculum | P2, separate learning records |
| Build: six-line project contract, acceptance gate, hackathon rubric, freeze/rehearsal/fallback, agent briefs | Retain | P2; basic project tags/notes in P0 |
| Care appointments and factual clinician summary | Retain user-entered scope, no clinical inference | P2 encrypted module and explicit export policy |
| Debrief and emotional regulation prompts | Pocket Reset now; richer optional debrief later | P0/P2, no automated diagnosis |
| People: callbacks, observation/request, collaborator roles | Retain optional, no buddy network | P2 local tool |
| Money Guard: discretionary cap, renewal tracking, purchase waiting period | Retain optional; protect care spending | P2 local tool |
| Capture/parking: text inbox, share target, voice/OCR | Scratchpad now; inbox/integrations later | P0/P2 |
| Regain, Blossom, BuddyHabit | Pattern inspiration only; no copied branding/assets | Usage pause, habit scheduling and presentation |
| OpenGym / Vital3D references | Investigate assets, license and fit before use | P2 research; no claim source was retrieved or integrated |
| Obsidian / calendar / health platform integration | Markdown export first; each deeper integration needs explicit scope | P0 export; P2 sync/import |

Deferred items remain part of the product backlog, not hidden promises of the expanded release. Before implementing one, define a bounded useful task, data ownership, export policy, accessibility states and evidence of success. Do not bring Expo's entire backend or native AI service into the core merely because it appears in an archive.

## 9. Delivery plan and evidence gates

The previous M01–M08 reports describe an earlier implementation with blocked builds. They are historical evidence, not proof of completion for either uploaded variant. The new sequence uses **R0–R7** to avoid confusing the two.

| Stage | Deliverable | Exit gate |
|---|---|---|
| R0 — Reconcile and build | Identify canonical repository, package/signing/schema; restore wrapper; freeze proven toolchain; establish encrypted stores and migration baseline | Clean checkout builds and installs on a capable host; no destructive migration; documented source comparison |
| R1 — Accessible shell | Shared components, five tabs, themes, onboarding, independent Safety | TalkBack/large-text checks; public Safety under lock and DB failure |
| R2 — Today and habits | Dashboard, task timeline, routines, historical occurrences, Minimum/Pause | Persistence and day-boundary tests; exact aggregate fixtures; undo works |
| R3 — Focus and notes | Session engine, queue, scratchpad, breaks, Pocket Reset | Actual-time accounting, process recovery, notification restrictions, duplicate prevention |
| R4 — Health | Manual workouts, food/water/sleep; gated step/GPS adapters | Edit/delete totals; permission/manual fallback; actual phone tracking evidence for enabled sensors |
| R5 — Distraction | Voluntary pause; usage/interception only if feasible | Xiaomi proof, bypass, exemptions, revocation and clear unsupported behavior |
| R6 — Review and portability | Historical charts, reflections, Markdown, encrypted backup/restore | Known-data totals, complete round-trip, Safety exclusion, atomic failure tests |
| R7 — Personal-use release | Device/accessibility/battery checks, long-history performance, reproducible APK and limitations | Every P0 accepted; P1 explicitly enabled/tested or deferred; install and recovery evidence attached |

Each stage reports commit, commands/exits, device/software, acceptance IDs, evidence, unresolved limits and next action. Use VERIFIED only for demonstrated criteria; IMPLEMENTED BUT UNTESTED for source without execution; BLOCKED with a concrete failed command and reason. Packaging a ZIP is not installation verification.

Parallel drafting is permissible; marking a dependent feature accepted without its prerequisites is not. A frozen build host is a delivery blocker to resolve through a capable authorized host or CI, not evidence that the source compiles. The phone must receive a real downloadable artifact; a remote `/workspace` path or unforwarded localhost address is not a download method.

## 10. Product validation and success measures

Do not add remote analytics to measure these goals. Use synthetic tests, voluntary user feedback and local summaries that the owner can inspect or delete.

| Question | Measure | Initial acceptance approach |
|---|---|---|
| Can the user get started easily? | Time/actions to begin a selected task | Onboarding walkthrough and three common daily scenarios |
| Does the app reduce administrative work? | User-reported logging effort and repeated friction | Short optional review after a week; simplify high-friction flows |
| Are records trustworthy? | Totals versus known fixture events | Exact agreement for time/counts; documented tolerances for sensors/estimates |
| Does it tolerate imperfect use? | Return after skipped days; Minimum/Pause use | No backlog flood, fake failures or forced catch-up |
| Is it accessible in real use? | Completion of essential journeys with large text/TalkBack | All essential journeys pass on the primary device |
| Can the owner recover data? | Eligible-record round-trip and failure preservation | All categories/relations preserved; Safety untouched |
| Is long-term operation practical? | Query/export times and storage growth with decade dataset | Meet agreed budgets; document large-export costs |

Do not make daily engagement time, streak length or number of logged fields the main success metric. The app should support work and life outside the app.

## 11. Risks, unresolved inputs and decision owners

| Risk / unknown | Response | Decision point |
|---|---|---|
| Uploaded native code differs from previously pushed implementation | Compare commits/files before porting; preserve stable identity | R0, developer |
| No verified build host | Prove local host or authorized CI before further acceptance | R0, developer/owner |
| Large feature scope | Deliver P0 vertical slices; gate P1/P2 separately | Every stage, owner |
| OEM background limits | Test actual device, expose limitations, retain manual fallback | R3–R5, developer |
| Encryption/key loss versus recovery expectations | Explain eligible backup and device-only Safety; test failure paths | R0/R6, developer/owner |
| Sparse or inaccurate health observations | Provenance, missing-data states, no clinical inference | R4, developer |
| Excessive dashboard complexity | Configurable modules, Minimum Day, readable list alternatives | R1/R2, design/owner |
| Asset/dependency rights unconfirmed | Inventory and license review before distribution | R0/R7, developer |
| Directory entries or platform policy changes | Recheck official references at release; date bundled content | R7, developer |
| Exact KDF/dependency/device performance unknown | Prove and pin using the technical gate; no invented certainty | R0/R6, developer |

Routine defaults in these documents allow implementation to proceed. Confirm only decisions that cannot be inferred from the repository or device, such as signing ownership, irreversible data replacement or an external paid service. Keep all unresolved facts visible in evidence rather than treating a polished document as proof.

## 12. Definition of done

The expanded personal-use release is done when its canonical source builds reproducibly, installs on the agreed phone, passes every P0 acceptance case, labels or omits unproven optional capabilities, preserves real records through updates and recovery, and completes essential journeys offline with accessible controls. It includes a versioned APK, source commit, dependency/asset inventory, test evidence and an honest list of supported conditions.

This documentation task delivers the product, design and technical specification plus source inventory. It does not build, publish or push an application, and makes no claim that either supplied prototype has passed those release gates.
