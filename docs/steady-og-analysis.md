# Steady OG integration analysis

Prepared 7 October 2026. Requested outcome: reuse the capabilities of the supplied
Steady OG project inside the existing Steady app. The latest owner instruction
also adopts its richer cards, graphs, water and habit animations and interactions,
preserving Kinetic/Daybook themes and using theme-aware text colors.
This is an analysis and implementation brief, not a completed merge or a new
build certification.

## Source and scope

Destination: `C:\Users\THANM\Documents\Codex\steady_app`.
Donor: `C:\Users\THANM\Documents\Codex\steady_app\steady og`.
The donor's `steady (2).zip` SHA-256 is
`d1ddd3bfa67b6e05884cfbb7126fb946f245df1e4f218c78342b3dbfe19ad290`.

The audit inventories 62 source, resource, test and configuration files. All 62
match their corresponding entries in the supplied ZIP byte for byte. The ZIP has
104 entries including directories. There are 35 production Kotlin files with
12,915 lines, and three test files declaring six tests. Line counts include
imports, comments and presentation code; they are not measures of feature quality.

Comparison with `Steady_Documentation/Source_Inventory.csv`, source N:
39 files are identical, 12 changed, and 11 absent from that earlier inventory.
The additions include the procedural sound engine, nature scenes, weekly chart,
habit tracker, study scheduler, meal suggestion card and reminder classes. OG
therefore contains real additions worth retaining; it is also closely related to
the native prototype whose weaknesses the documentation already identified.

Reviewed feature definitions, ViewModel actions, repository/DAO behavior,
navigation call sites, chart calculations, audio generation, manifests, build
configuration and tests. This was a static inspection. OG was not compiled,
installed, exercised with a real Gemini key or certified as fully functional.
No personal database, private Safety record, API secret or signing key was read.

Inventory: [source hashes](steady-og-source-inventory.csv).
Capability map: [feature checklist](steady-og-feature-matrix.csv).
Implementation instructions: [merge prompt](steady-og-merge-prompt.md).

## What to reuse

Both projects are native Kotlin/Compose apps with one app module. There is no
need to convert a web app or recreate the entire product. Reusable work includes
sound-sample generation, recipe and proposal inputs, chart interaction behavior,
plan adoption workflows, reminder controls, grounding steps, and supporting data
transfer objects. Existing destination equivalents should be extended in place.

The destination already has richer persisted activity timing, encrypted stores,
habit occurrences and historical versions, logical-day handling, migration
history, file recovery, authentication, subjects, workout sets and templates.
Replacing those with OG's simpler implementations would lose existing behavior.

The current visual direction retains `SteadyTheme`, `ExpandedNavigation`, the five
destination tabs, typography, insets, accessible controls and customization.
Adapt OG's richer layouts and animations, including meaningful graph dimensions,
selection and drill-down behavior, using destination colors and components.
Fixed black/blue text must become theme-aware. This supersedes the earlier request
to preserve the destination's existing card layouts unchanged.

| OG area | Useful capability | Destination integration |
|---|---|---|
| Today | Task completion, priorities, time slots, duration, decomposition and plan adoption | Existing `ExpandedToday`, task editor, `PlanItem`, repository and timeline |
| Scheduler | Three block proposals, primers, adopt, reminder toggle and AI personalization | Editable proposals in Today; persisted IDs/times; existing reminder platform |
| Habits | Completion summary, current/best streak presentation and seven-day history | Existing occurrence/version/log data; optional non-punitive summaries and genuine dated history |
| Blossom | Hydration, focus, pause and movement mini summaries | Existing dashboard cards; real supporting records rather than fixed petal counts |
| Kinetic | Three progress channels with animated arcs and legends | Existing configurable dashboard metrics/rings; additional supported metrics only with valid data |
| Circadian | Daily horizon, event markers and current-time indicator | Existing task/activity timeline; explicitly entered energy if supported, never inferred physiology |
| Break | Four focus/break presets, phase controls, subject, recent sessions, dim surface | Existing `ExpandedFocus` and persisted activity engine |
| Audio | Six procedural sounds, volume, timer linkage, three scene choices and full-screen portal | Replace the destination audio stub; bind current controls to real playback |
| Health | Five mode ideas, manual reps, workout history and deletion | Existing seven modes, actual time, manual sets, interval program and templates |
| Review | Focus/rest bars, totals, focus ratio, tap detail, reflection and past reviews | Existing week/month Review, real active segments and supporting records |
| Safety | Public directory, private note categories/pins, contacts, masking, grounding, dial/composer | Existing independent public shell and separate encrypted private store |
| Settings/files | Counts, export preview, copy/share intent, backup and restore intent | Existing SAF, encrypted archive and strict organiser-only scope |
| Gemini | Task decomposition, meal suggestions/search, schedule proposals, reflection rewriting | Separate optional connected implementation; owner-selected Gemini BYOK, outbound preview and editable apply |

## Reachability matters

OG navigation is `Today`, `Break`, `Safety`, `Review`, `Settings`. `HealthScreen`
is defined but has no invocation in the supplied production source. The same is
true of `KineticTriRingsCard` and `BlossomHabitMatrixSection`. Their presence does
not establish a usable journey. The merge checklist includes them so they cannot
disappear merely because they were dormant.

The scheduler, habit tracker, meal generator, circadian chart and practice pause
are invoked from Today. The sound card/portal are connected to Break's sound
engine. The weekly chart is invoked from Review. Public/private Safety flows and
Settings file actions are reachable, with the defects below.

## Data and component mapping

The donor has ten Room entity types. Do not add all ten as a second parallel
schema. Map their useful fields to the current records, extending a record only
when its existing contract cannot express the capability.

| Donor type | Destination owner | Important conversion |
|---|---|---|
| `PlanItem` | Existing `PlanItem` | Auto-increment ID to stable mapped string ID for explicit legacy import; category/priority enums; text time slot to explicit owner-selected time; planned minutes to planned seconds |
| `FoodIdea` | `FoodIdeaRecord` | Preserve prep time, ingredients, instructions, favorite and origin; do not import unsupported benefit promises as verified facts |
| `StudySession` | `ActivitySession` and `ActivitySegment` | Preserve imported duration as a legacy recorded entry, not reconstructed sensor/timer proof; calendar date retains legacy provenance; do not synthesize break records |
| `SafetyNote` | Extension of the separate private Safety schema | Additional note/category/pin fields belong here; never a general organiser import/export table |
| `DailyReview` | `Reflection` | Preserve optional owner ratings/text and date; distinguish generated suggestion from user reflection; preserve a current reflection on merge conflict |
| `AppSetting` | Encrypted preferences/profile; narrow bootstrap only where valid | Typed keys and validation; no arbitrary plaintext settings or client key copying |
| `Habit` | `HabitDefinition`, `HabitVersion`, `HabitOccurrence`, `HabitLog` | Definition-level streak/completion cannot reconstruct reliable dated history; do not generate past completed days from a streak count |
| `HabitItem` | Existing habit/metric view models | No donor DAO exposes these items; treat as a dormant display model, not a complete second habit system |
| `WorkoutSession` | `ActivitySession`, `ExerciseSet`, optional `EnergyEstimate`/`ActivityObservation` | Manual notes/reps and actual entered time; imported simulated steps/calories cannot be relabeled observed |
| `KineticDailyTelemetry` | Derived dashboard metrics and explicit preferences | Goals map to optional target settings; seeded/incremental totals do not become an authoritative source alongside individual logs |

`ScheduledStudyBlock`, `DayFocusBreakMetric`, `BrainMealSuggestion`, `ExerciseMode`,
`TimerMode`, sound/scene enums and their UI state objects are useful transfer or
presentation models, not durable storage by themselves. Derive chart/summary
models from repository records, persist proposal identity where necessary, and
capture immutable request/completion snapshots. Stable imported ID namespaces
must preserve links without colliding with existing destination records.

Source food and review JSON lacks stable entity IDs and complete timestamps;
an importer must define deterministic mapping and conflict policy rather than
guessing new personal history. Source seeds are templates/example activity,
not evidence that the owner completed them. Merely moving an `@Entity` declaration
does not create migrations, DAO operations, backup support or a usable feature.

## Corrections required during reuse

### 1. The weekly chart has good interaction, inaccurate source data

`ReviewViewModel.kt:80` constructs a seven-day window. At line 90 it substitutes
110/135/90/150/120/75 study minutes for earlier dates without actual sessions.
At line 100 it invents rest as 22% of study time. Session counts are also inferred.
`D3WeeklyFocusChartCard.kt:55` displays an 80% focus ratio for an empty period.

Keep paired study/rest bars, minute axes, legends, weekly totals, selected-day
detail, ratio and responsive layout. Feed them actual focus and break records.
Show no ratio when its denominator is zero. Do not count a break preset as an
observed break. Connect selections to supporting records and an accessible list.

The component loads D3 7.8.5 from a CDN at line 291 and has a local SVG fallback.
Its comments mention grouped/stacked rendering, but the implementation presents
grouped bars, without a chart-mode selector. Preserve the actual grouped-bar
behavior. The offline build must not depend on that CDN. A compatible local
renderer can be reused after license review, or the existing native rendering
can implement the same interaction. Arbitrary personal text must never be
interpolated as executable HTML. Changing only the data source is insufficient
if the final graph still needs the internet or lacks accessible selection.

### 2. Rings and horizon are mostly presentation

`Entities.kt:130` defaults telemetry to nonzero calories, steps, active time,
water and pause counts. `AppDatabase.kt` also seeds populated telemetry and
workout history. The tri-ring badge says sensors are live without a sensor
implementation in that flow; its ticker fixes calorie rate, elevation and gyro
status. All three `drawArc` calls omit explicit bounds while their background
circles have different radii; check geometry rather than blindly copying it.

Retain progress-channel functionality, numeric legends and detail actions.
Guard empty/zero goals and non-finite values. Data needs provenance and genuine
records. Missing sensor data is unavailable, not a seeded achievement.

`CircadianHorizonChart.kt` takes no data argument. Its Bézier curve, beacon and
schedule nodes use fixed positions and text. It cannot establish circadian
readiness, a chronotype or a current physiological surge. Keep the daily-horizon
interaction and actual event markers using the destination timeline. A factual
energy graph needs deliberately entered observations; do not manufacture them.

`BlossomHabitMatrix.kt` renders fixed focus petals, posture resets and streaks;
the passed habit list mainly supplies a count. Bind every petal/summary to its
underlying task, habit occurrence or activity. “Add water” must create an actual
serving log with undo, not mutate a capped daily float.

### 3. Scheduling and habit state need durable identity

`TodayViewModel.computeSuggestedBlocks` returns three fixed windows and
physiological scores derived partly from streak counts. Adoption is inferred
through title substrings, so renaming or repeated taps can produce duplicates.
Reminder IDs and the global enabled setting live only in UI state. Turning the
global switch off does not cancel existing alarms. The notification permission
launcher in `StudyBlockSchedulerCard.kt` ignores its callback result.

Preserve editable time/duration/task/primer proposals and adopt/reminder actions.
Store stable proposal/plan IDs and explicit adoption state. Do not present
template times or arbitrary cognitive scores as personalized measurements.
Persist reminder enablement; disabling/rescheduling/deleting cancels stale alarms.
Handle grant/deny/revocation and quiet hours without losing the proposal.

OG habit completion increments/decrements a stored streak but does not verify
consecutive scheduled dates or reset `isCompletedToday` at a new logical day.
Its seven-day dots infer history from a streak count using fixed weekday labels.
Use destination occurrences and effective versions for actual dated history;
undo must be idempotent. Optional current/best consistency summaries require
defined scheduled-day semantics, with Pause/Minimum/unscheduled exemptions.

### 4. Timers and workouts must retain actual elapsed time

`BreakViewModel.kt:152` skips into the next phase through the full-session logger.
That logger records the configured study duration, a rating of five and a
“completed full” note. Timers are in-memory `CountDownTimer` instances. They do
not prove recovery after recreation/process death. The phase switch and
asynchronous logger can also read mutable current state at different times.

Reuse presets, control intentions and audio coupling. Use the destination
activity repository, monotonic clock anchors, segments and generation checks.
Stop/skip records actual partial time; ratings remain optional and user supplied.
Breaks must be stored separately if the chart is to report actual rest.

`HealthViewModel.kt` assumes 70 kg, ticks elapsed time by callback count, calculates
calories from a fixed MET, forces at least one minute/ten calories, and invents
walk steps as minutes ×120. Deleting a workout does not reverse accumulated daily
telemetry in `SteadyRepository`. No pedometer, GPS or genuine automatic repetition
detection is implemented by these labels. The “OpenGym” mode is a mode name and
description, not integration of the separately supplied OpenGym project.

Keep manual modes/reps/notes/history and destination sets/rest/interval behavior.
Observed steps need a working permitted sensor; estimates remain distinct and
require actual user inputs. Derive totals from records so edits/deletes propagate.

### 5. Sound generation is reusable, playback lifecycle needs work

`AmbientSoundEngine.kt` generates mono 44.1 kHz PCM with real white noise,
filtered rain noise, brown noise, wind/birds, a 432 Hz modulated tone and campfire
texture. Unlike the current destination audio stub, it actually writes audio.
The source label “Alpha binaural” is inaccurate: the audio format is mono and
the tone uses 10 Hz amplitude modulation, not separate left/right frequencies.

Port compatible sample-generation logic and give it honest sound labels. Handle
initialization and write failures, audio focus, interruption, route loss,
cancellation and release. `isPlaying` must reflect successful playback. Avoid
stop/start races where a canceled writer accesses a released track. Timer audio
is opt-in and independent of completion cues. The wave bars are decorative
animations, not measured audio analysis or physiological activity.

For the destination's target API 35, requesting audio focus requires being the
foreground app or running an appropriate foreground service. This makes the
chosen background-playback policy part of implementation and device verification.
See the official [Android audio-focus guide](https://developer.android.com/media/optimize/audio-focus),
checked 7 October 2026.

The three vector scene resources exist and are already also present in the
destination. Check hashes/provenance before duplicating them. Use current card
and full-screen styling; preserve scene choice, sound association, play/stop,
volume, close and Safety. The source's image-quota dialog is fixed copy, not a
working image-generation feature. Do not migrate that claim.

### 6. Safety, credentials and portability cannot be transplanted wholesale

OG uses ordinary Room, destructive migration and disabled schema export.
`MainActivity.kt:60` creates the database before exposing any route. Manifest
backup is enabled, with template XML exclusions. Its PIN only updates UI state;
contacts and selected region also remain in ViewModel state. Private notes share
the organiser database. Region groups and numbers are unverified here.

Android documents that destructive fallback can remove stored data when a migration
path is missing. Preserve explicit migrations and test supported upgrades using
the destination version's APIs; the newest documentation samples are not a reason
to upgrade its pinned Room stack. See [Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions),
checked 7 October 2026.

Reuse note categories, pin/mask actions, the grounding sequence, country-choice
intent and dial/composer flows inside destination security boundaries. Public
help must survive lock/auth cancellation and either private-store failure.
Personal contacts stay private. A region directory requires official verification
of each service and supported contact method before release. SMS is not suitable
for every number labeled “crisis.” No automatic call/message is authorized.

OG Markdown and JSON exports include private Safety. JSON export includes foods
and reviews, but restore handles only plans, Safety and sessions. It omits IDs,
is non-atomic and repeated imports duplicate data. Habits/workouts/telemetry are
not covered by that portable format. Keep destination encrypted backup, preview,
strict validation and SAF. Ordinary exports/backups always exclude Safety and
contacts. If OG organiser-file import is wanted, make it explicit, bounded,
transactional, repeat-safe and previewed; reject/ignore its `safetyNotes` subtree
without logging its content. Do not copy either database over the owner store.

Gemini service methods make genuine HTTP requests in the source, but their API
success/model availability were not tested. They embed `BuildConfig` keys in
request URLs and surface raw error bodies. Output is loose text/marker parsing,
with made-up recipe defaults. Search query strings are labeled “verified” without
retaining actual citations. Schedule/review generation can finish after the user
changes date and act on newer mutable state.

Retain all five proposal intentions: task decomposition, recipe search, a
time/pantry-constrained meal, schedule personalization and reflection rewriting.
Implement Gemini BYOK only in the separately disclosed connected build. Verify
models and parameters against official documentation at implementation time.
Preview exact outgoing fields; exclude Safety/contacts/credentials; bind requests
to their captured target date and draft revision. Validate structured results,
allow editing, and apply only selected results transactionally. Offline planning
remains usable without a key. A key supplied through the app must not become a
source file, exported credential or log line.

Google cautions that keys compiled into mobile clients can be extracted and
recommends server-side calls for distributed production apps. Personal BYOK must
disclose that limitation; it cannot offer the same secrecy as a server-held key.
See [Gemini key security guidance](https://ai.google.dev/gemini-api/docs/api-key),
checked 7 October 2026. This is a security boundary, not a live model/API certification.

## Destination reconciliation discovered in this inspection

The working tree is dirty at Git HEAD
`862b8d1ca68cca2a74dca17084094dad651be29a`. Existing edits belong to the ongoing
project and must not be discarded, automatically staged or misattributed.
The current source is ahead of that commit and of earlier APK/test evidence.

Current files include:

- `AudioSoundscapeEngine.kt`: explicitly mocked playback without media resources.
- `PlatformSensors.kt`: hard-coded 168 SPM after any step delta; permission/lifecycle
  handling and persistence are incomplete. This is not verified cadence.
- `ExpandedNavigation.kt`: sample usage map for a made-up package on its usage route.
- `ExpandedUsageInsights.kt`: claims active monitoring without demonstrating a
  functioning interception service.
- `ExpandedFocus.kt`: fixed study sets, rating slider with an empty callback,
  neural-audio labels and illustrative scratchpad text alongside real notes.
- `ExpandedHealthWidgets.kt`: fixed curve, posture/IMU claims and fixed nutrition
  ratios/score.
- `ExpandedP2Entities.kt`: entity declarations absent from the actual Room
  database entity list; this alone does not implement those modules.
- `ExpandedP2Backlog.kt`: presentation/no-op capture stubs, not completed workflows.
- `docs/evidence/m09.md`: claims full verification despite describing scaffolds
  and a test command as still running. It is not sufficient acceptance evidence.

The implementation prompt requires reconciling these honestly while preserving
their approved visual structure where useful. It must not use mock features or
this document as proof a milestone passed. Earlier 27-unit/27-device checks apply
to the corresponding earlier checkpoint, not automatically to all current edits.

The official [UsageStatsManager reference](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
describes permission-gated event/statistic queries, which can have missing data
while the device user is locked. Those queries require their own honest unavailable
states. The conclusion that this project's query adapter does not implement an
interception service comes from its source inspection. Reference checked 7 October 2026.

## Build and data boundaries

Destination identity is `com.thanu.steady`, namespace `com.thanu.steady`, minimum
API 26, compile/target API 35, JDK 17, current Room schema 5. The donor identity is
`com.aistudio.steady.stdyapp`, namespace `com.example`, minimum API 24, target 36,
Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10 and Java 11. Donor wrapper launchers/JAR and
the configured `debug.keystore` are absent. Do not import its build wholesale.

Reuse within the destination's compatible stack; small Compose API adjustments
may be needed. Keep the package and signer so owner updates retain data. Use the
isolated `.qa` package for tests, never the owner's database. No owner uninstall,
clear-data or `connectedDebugAndroidTest` during validation.

No project license/NOTICE was found in the inventoried OG files. This inspection
does not establish redistribution rights for source, vector assets or external
chart code. Record supplied provenance and review the particular material reused;
do not silently relabel it as newly authored/licensed. The OpenGym ZIP is a separate
donor and must retain its separate source/license audit.

## Recommended merge order

1. Preserve the actual current work; establish baseline build/evidence and a
   complete source-to-destination checklist. Keep UI screenshots limited to
   synthetic organiser screens; never capture private Safety.
2. Bind source-backed dashboard/habit/timeline/review features to current records,
   then add actual break history required by the focus/rest graph.
3. Port procedural audio and its complete lifecycle; reuse scene assets once.
4. Add durable block proposals/adoption/reminders and recipe-to-plan actions.
5. Extend private Safety notes/pins/masking and optional grounding without coupling
   public help to the database; correct file compatibility as needed.
6. Add the separate Gemini BYOK proposal flows, with structured preview/edit/apply.
7. Verify every feature and retained destination behavior, document actual gates,
   then update the owner APK using `adb install -r` only.

Each source feature has a checklist row. A feature can reuse a stronger destination
implementation, require adaptation, belong to the connected variant, or be a
source claim that must be corrected. Account for all of them; do not silently omit
features, replace current UI with donor screens, or declare scaffolding complete.
