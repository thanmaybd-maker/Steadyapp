# Steady — Detailed Design Specification

**Version:** 2.0 · **Date:** 6 October 2026 · **Status:** target experience and implementation specification.

Read with `Steady_Tech_Stack.md` and `Steady_PRD.md`. This document merges the visual and behavioral evidence from both code ZIPs, all seven Stitch exports, the prior architecture and the two pasted briefs. It specifies what to build; it does not certify that the uploaded prototypes already behave this way.

## 1. Experience direction

Steady helps a person choose a manageable next action, focus on study or building, record movement and daily routines, and review what actually happened. It is for ongoing daily use. The earlier three-week build target does not create an expiry, challenge countdown or compulsory streak.

Combine two complementary directions:

- **Kinetic:** the emerald/indigo rings, modular dashboard, clear workout sets and meaningful charts in Stitch.
- **Daybook:** the Expo implementation's warm paper, orbit mark, humane reflection, routine anchors and Pocket Reset.

Use one component system with two palette choices. Kinetic is the initial visual default because the user requested a richer interface. Daybook is an optional warm appearance using the same navigation and behavior. Dark and high-contrast variants apply to either. Avoid implementing two unrelated apps behind a theme switch.

Visual richness comes from layout, typography, real progress and responsive controls. Do not fill empty space with invented physiological scores or decorative live indicators. Key actions must be understandable without knowing terms such as telemetry, dopamine velocity or circadian coherence.

## 2. Source-to-design map

| Source | Preserve | Adapt or remove |
|---|---|---|
| S0 Health | Mode selector, hydration card, activity history, food section | Replace posture/circadian/sweat/glycemic measurements with actual logs |
| S1 interruption | Prominent return-to-study, intent choices, adjustable pause | No forced breath holding, hidden exit, false saved-time claim |
| S2 Focus | Subject queue, prominent timer, scratchpad, audio controls | Remove heart rate, cognitive velocity, unverified neural effects |
| S3 Today | Three rings, editable timeline, card hierarchy, quick water action | Real data sources; editable targets; no fixed Alex/chemistry examples |
| S4 workout | Exercise sets, weight/reps, rest timer, mode cards | Manual set recording first; no pocket bench accuracy claim |
| S5 Focus | Cleaner timer, compact controls, optional break cards | Keep generic comfortable pause cues; no treatment claims |
| S6 Review | Habit grid, period tabs, separated summary cards, export | No percentiles, invented p-values or causal conclusions |
| E Expo | Orbit logo, paper palette, routine anchors, low-pressure copy, Pocket Reset, export preview | Enlarge small metadata; preserve contrast in dim view; remove Safety export toggle |
| N native | Task categories/priority, food editor/favorites, review fields, Safety categories, mode concepts | Repair false sensor labels, seed data, faux PIN, unwired routes and privacy claims |
| A original | Minimum Day, Pause, accessible cues, independent Safety, lifetime history | Expand tab structure and optional fitness as explicitly requested |

All seven Stitch DESIGN files are identical. Use their shared visual vocabulary once. Specific screen HTML sometimes differs from those tokens; the resolved tokens below take precedence.

## 3. Navigation and screen architecture

Main tabs are **Today, Health, Focus, Review, Settings**. Each tab preserves scroll position and relevant filters. Use standard back behavior: close sheet/editor, return from a detail page, then leave the root; do not force every Back action to Today.

A labeled **Safety** action occupies a consistent top-bar position. Active focus/workout and dim views also expose it. It opens an independent route with public help first. A private-store failure, app lock, missing permission or active modal must not prevent reaching public help. Sheets place Safety in the visible header or close directly into a screen where it is reachable.

| Area | Primary surfaces | Secondary surfaces |
|---|---|---|
| Today | Greeting, next action, rings, timeline, habits | Plan editor, habit editor, dashboard layout, capture, Minimum Day |
| Health | Movement, workouts, food/water/sleep cards | Mode picker, active workout, exercise editor, recipe detail, log editors |
| Focus | Session setup or active session, task queue | Subject/topic detail, scratchpad, dim view, Pocket Reset, distraction rules |
| Review | Week/month summary, habit matrix, reflection | Day detail, metric history, selected export preview |
| Settings | Appearance, accessibility, modules, permissions, privacy | Backup/restore, logical day, data management, optional integrations |
| Safety | Public country-specific help, private plan | Contact editor, note editor, optional grounding cue |

Learn, Build, Capture and later personal records live as focused detail areas under existing tabs rather than increasing the bottom bar. A parked idea can be captured from Today without creating a scheduled task.

## 4. Visual foundations

### 4.1 Color tokens

| Role | Kinetic light | Daybook light | Dark starting value |
|---|---|---|---|
| Background | `#FAF8FF` | `#F4F0E6` | `#111A18` |
| Card | `#FFFFFF` | `#FFFCF6` | `#1B2823` |
| Primary text | `#131B2E` | `#233A33` | `#F2F5F3` |
| Secondary text | `#475569` | `#465B52` | `#B9C9C0` |
| Primary action | `#006C49` | `#25483D` | `#82DAB3` with dark text |
| Habit / movement accent | `#10B981` | `#91A68A` | `#4EDEA3` |
| Focus accent | `#6366F1` | `#5967A1` | `#C0C1FF` |
| Estimated calories accent | `#F59E0B` | `#D9784A` | `#FFB95F` |
| Water accent | `#06B6D4` | `#4F8A91` | `#67D8E8` |
| Error text | `#9B1C1C` | `#9B1C1C` | `#FFB4AB` |

Accent colors are for rings, indicators and surfaces; they are not automatically safe foreground text. Use darker semantic text on pale fills. The prototype's bright emerald gradient with white text must be contrast-tested or changed. High-contrast mode removes translucent surfaces, glow and gradient text, strengthens boundaries and uses tested text/background pairs.

Essential text target: 7:1 contrast; all normal text at least 4.5:1, large text at least 3:1; essential non-text controls and chart distinctions at least 3:1. These are product acceptance thresholds, not a declaration that every proposed token pairing passes. Document the actual rendered combinations.

### 4.2 Typography, geometry and motion

Bundle Plus Jakarta Sans if the asset licence is verified; otherwise use the platform sans-serif. Optional JetBrains Mono is limited to large timer/numeric readouts, never a requirement for readability. Daybook may use a local/platform serif for large headings only.

| Element | Default size / line height | Rules |
|---|---|---|
| Timer | 48–64 sp / fit line | Tabular digits; scales/reflows without clipping |
| Screen title | 28–32 sp / 36–40 sp | Sentence case; no hardcoded single-line limit |
| Section title | 20–22 sp / 28 sp | Clear relationship to following content |
| Body/control label | 16–18 sp / 24–28 sp | One primary action per local group |
| Metadata | 14 sp / 20 sp | Never reduce essential information to 9–11 px |

Spacing scale: 4, 8, 12, 16, 24, 32, 40 dp. Page gutter 20 dp on normal phones, 16 dp on narrow widths. Standard card radius 20 dp, hero radius 24 dp, field radius 12–16 dp. Interactive target minimum 56 × 56 dp, including icon buttons and navigation items. A drawn checkbox can be smaller inside this hit area.

Use 160–240 ms fades or small position/scale changes for completion and expansion; avoid perpetual pulsing. Reduced motion renders immediate changes or simple opacity. Haptics are optional and off when system/user preference requires. Never use flashing to force attention.

Keep shadows modest and use mostly opaque cards; expensive full-screen blur is not required to reproduce the visual intent. Decorative glows must not reduce contrast or obscure hit boundaries.

## 5. Onboarding

1. Welcome with optional name. Explain that core records are stored on the device and exported only by explicit action.
2. Select desired areas: daily plan, study/build focus, habits, movement, food/water/sleep. All can be changed later.
3. Choose appearance, comfortable text scale and motion preference. Preview actual controls.
4. Confirm timezone/day boundary and country for public help. Suggest India for this personal setup, but show an explicit editable selection. No invented contacts or health facts.
5. Offer a small example routine as an editable template. Choosing it creates pending tasks only, never completed history.
6. Enter Today. Request sensor/location/notification/usage permissions only when the relevant feature is used.

Body weight, calories and workout targets are optional. No need to enter relationship, religion, income, diagnosis or private family information. No mandatory account, email, API key or wearable pairing.

## 6. Today specification

Default order: greeting/date → next action → three rings → timeline → routine/habit cards → optional check-in → food idea. Safety stays in the top bar. A persistent add control opens a short menu for Task, Habit, Capture or Log; it never hides navigation.

### Rings and chart

Defaults: steps, focus minutes and scheduled habits completed. Tap a ring to open its detail history. Long-press or use the visible Edit dashboard control to choose another supported metric. Calories require the optional estimate feature. Each legend shows value, goal if configured, unit, source and last update when freshness matters.

If steps are unavailable, show “Steps unavailable — set up tracking,” not 0 with a completed-looking ring. No goal means a neutral ring and absolute value. Values above a goal remain visible; the ring caps at one revolution with text such as “45 / 30 min.” There is no obligation to raise the goal.

The timeline plots scheduled items and actual logged segments on a labeled time axis. Energy ratings, when present, are shown as separate self-reported points; do not interpolate a physiological forecast. Overlapping items stack or group with an explicit count. Tap, accessible list actions and keyboard navigation all reach the same editor.

### Tasks and habits

A task row includes title, optional category, planned time and duration, completion state and overflow actions. Actions: complete/undo, edit, reschedule, start focus and archive/delete. Notes are secondary. Deleting shows an Undo snackbar; deleting a definition that has history archives it.

Habit types: checkbox, count, quantity and duration. Schedule: daily, selected weekdays or a chosen interval. The editor captures a name, type, unit, target, schedule and optional reminder. Do not require a motivational quote or streak goal. Quantity entry supports correction and undo. Water uses integer milliliters internally and user-selected display units.

Routine anchors group actions around “After waking,” “After college” and “Before sleep,” copied as editable ideas from the earlier plan. Checking an occurrence affects that day only. Renaming a habit today does not rewrite old labels in Review.

### Minimum Day and Pause

Minimum Day offers a small study step, a small build step and optional comfortable movement/rest. The earlier 10/10/5-minute suggestion is editable guidance, not a prescription or pass/fail threshold. Hide nonessential cards while keeping access through a collapsed section.

Pause stops routine reminders and creates no catch-up debt. Resuming asks for the next manageable action without replaying missed reminders. These modes remain active after relaunch and are visible in the header.

## 7. Health specification

Health has an overview and optional sections for Movement, Workouts, Food, Water and Sleep. A user can hide any section. There is no default composite health score.

### Movement and workouts

Mode picker: Walk, Run, Cycle, Strength, Intervals, Mobility, Custom. Each card explains what will be recorded and whether permission is needed. Start flow shows mode, optional target, data source and Start. Location denied still permits a duration-only workout, visibly labeled.

Active outdoor screen: elapsed active time, distance, pace and pause/finish controls. Source status appears as “GPS recording,” “Location weak,” or “Duration only.” Route detail uses genuine recorded points; missing segments are visibly broken. Hide precise location on lock-screen notification text.

Strength screen follows S4: routine name, exercise cards, set rows, weight/reps, completion check and rest timer. Editable rows support bodyweight and custom exercises. Save sets as entered. Add/remove/reorder exercises without losing finished sets. Exercise library content needs verified reuse rights before shipping; OpenGym is currently a reference request, not imported code.

Intervals allow work seconds, rest seconds, rounds and warm-up/cool-down if chosen. A clear current phase and optional sound/haptic cues make them usable with the screen off once tested. Mobility is a checklist/timer with user-defined movements, not camera or spinal analysis.

Finish shows actual duration, recorded sets/route, optional effort and any calorie estimate with assumptions. Save, discard and resume are explicit. Do not fabricate a minimum one-minute workout or ten calories. Editing/deleting a session updates Today and Review.

### Food, water and sleep

Food cards show name, photo if bundled, preparation time, ingredients, rough budget tag and favorite control. Detail adds steps, substitutions and user-entered avoid-food tags. Indian staples and low-preparation options should be included alongside imported recipe inspiration: rice/dal/vegetables, curd with fruit, eggs or beans/toast, and leftovers are examples, not a therapeutic plan.

Rename “Brain Fuel” to “Food ideas.” Replace claims that foods prevent brain fog, guarantee no glucose spike or sharpen recall with concrete descriptions of ingredients and preparation. No automatic vitamins, supplement doses or electrolyte calculation. A care reminder merely repeats instructions entered by the user.

Water offers configurable quick quantities, undo and history. Avoid enforcing the prototype's universal 3 L target or 5 L logging cap; permit correction and a clear confirmation for an unusually large entry. Goals are optional preferences. Sleep is entered by the user with start/end and optional restfulness. Overnight intervals handle date changes.

## 8. Focus and Pocket Reset

Focus setup has a chosen subject/task, preset or custom duration, optional break duration, and optional distraction settings. Presets: 15/3, 25/5, 50/10, 90/20 plus custom/open-ended; presets are selectable tools, not a recommended medical schedule. Study and Build are session categories with the same timer engine.

During a session, the timer dominates. Primary action is Pause/Resume; Stop remains visible. Show current task, a compact queue, and scratchpad below. Audio controls are collapsed until enabled. Avoid displaying medical-looking indicators around the timer.

Stop early shows actual completed time and choices “Save partial session,” “Resume,” or “Discard.” Skip to break does not report a full study block. After completion, offer a break and one optional outcome note; never auto-fill a five-star rating. A completed notification must not create a second session if the app is also open.

Dim view uses a near-black background with readable timer/controls, optional screen-brightness reduction limited to the app window, and visible Exit/Stop/Safety. Restore previous brightness on exit. Never require touching invisible targets. TalkBack does not announce every second; announce meaningful phase changes and user-requested remaining time.

Pocket Reset adapts E's three steps: **Pause → Breathe comfortably → Choose one next step**. It is available from Today, Focus and Safety. The user can skip or close any step. No forced breath retention, physical challenge or treatment claim. Optional generic stretch/eye-rest cues can be disabled; they do not override the user's care instructions.

Scratchpad supports plain text and a restrained Markdown subset. Autosave with clear Saved/Saving/Retry status; navigate away without losing text. Formula entry stays text unless a well-tested renderer is added. Export is a deliberate action; no fake “Synced to Obsidian” label.

## 9. Distraction pause

S1 informs the layout: selected app name, simple pause indicator, optional intent choices, Return to focus, Continue temporarily and Disable protection. Do not force the user to disclose why they need an app. Essential apps are exempt.

Pause durations may be 3, 5, 10 or 15 seconds, editable. Copy: “Pause for a moment. What would you like to do?” Breathing is optional. Temporary access shows the remaining allowance only where the implementation can actually enforce it; otherwise it is a reminder, labeled accordingly.

State distinctions: voluntary practice; usage access enabled; interception active; permission missing; service stopped; unsupported device. Only show an Active badge when the supporting service reports that state. Practicing the screen must not count as an app being blocked. Insights count observed attempts, returns and bypasses; “time saved” is absent unless a defensible explicit estimate is later designed.

## 10. Review and reflection

Week/month selection includes previous/next, calendar picker and Today. Cards show focus duration by subject, habit completion matrix, workouts and optional water/sleep history. Each card opens underlying records and can be hidden.

Habit cells distinguish completed, partial, skipped, not scheduled and missing, using shape/icon plus text as well as color. Provide a linear list alternative for TalkBack and large text. Tap a date to inspect/correct records. Charts expose actual units and ranges and never compare with a fabricated population percentile.

Reflection uses optional fields: What helped? What was too demanding? What evidence changed? What is one adjustment? Retain the native prototype's highlight, obstacle and tomorrow-priority ideas as optional prompts, not a second compulsory form. Computed history updates must preserve written reflections.

Examples: “You logged three focus sessions on two days.” “No sleep entries for this period.” Avoid “Your cognition improved 22%,” “optimal equilibrium” and “clinical diagnosis.” The archived Expo `weeklyReflection` offers useful tone but should not substitute for the new historical data model.

## 11. Safety, settings and portable files

Safety first renders selected-country public help from bundled data, with country and last content-review date. Labels accurately identify service and region; no unverified universal directory. Tapping Dial opens the system dialer. Optional SMS opens the composer with a preview; Steady never silently sends a message. Personal trusted contacts are clearly distinguished from public numbers.

Private sections: warning signs, coping steps, care contact, environment notes and user-entered medical details if desired. All begin empty. The native archive's seeded allergy and blood-type card must not ship as a user record. Show private-store errors separately while public help remains usable.

Settings groups: Appearance; Accessibility; Modules and dashboard; Time and reminders; Tracking permissions; Privacy and lock; Export and recovery; About and diagnostics. Permission pages explain each feature and permit disabling it without disabling the planner. Optional app locking uses real platform authentication.

Export flow: choose date range/categories → preview → explain readable Markdown → choose destination → confirm result. Safety is excluded by product contract, with no inclusion switch inherited from E. Backup flow adds a passphrase and explains its recovery role. Restore previews eligible data before replacement and explicitly preserves destination Safety. Deletion explains that external exports and already-shared files cannot be erased by deleting local app data.

## 12. Interaction and state contract

| Component | Interaction | Essential alternate states |
|---|---|---|
| Progress ring | Tap detail; accessible numeric summary | No target, unavailable, stale, over target |
| Habit card | Check/log/undo; edit from overflow | Pending, partial, done, skipped, not due, save failed |
| Timeline item | Open/edit/start; move via controls | Conflict, past, unscheduled, paused day |
| Timer | Start/pause/resume/stop | Recovered, late cue, notifications denied, interrupted |
| Workout row | Log/edit set; rest timer | Invalid units, unsaved input, partial session |
| Safety action | Open public shell directly | App locked, DB failure, offline, no dialer |
| Save notice | Confirm durability | Pending, failed, retry; never false success |
| Export | Preview and destination selection | Cancel, provider revoked, disk full, write failed |
| Restore | Preview counts then confirm | Wrong password, corrupt, unsupported, oversized |
| AI proposal, later | Preview/edit/select/apply | Offline, canceled, malformed, rate limited, untrusted source |

Errors appear adjacent to the relevant action with a next step. Loading placeholders never display fake numbers. Disabled actions explain why. Undo is available for ordinary logging changes; destructive restore/delete uses clear confirmation. Haptic feedback is supplemental, not the sole confirmation.

## 13. Responsive and accessibility acceptance

Phone portrait is primary, but rotation is supported. At narrow width or large font, cards collapse to one column, chip rows wrap, numeric legends become vertical and fixed-height containers grow. At 600 dp+ use a rail/two-pane layout where it improves reading; no web-style twelve-column dashboard on a small phone.

Test TalkBack traversal order, control names, chart descriptions, focus return after sheets, switch states, keyboard operation, 200% text and the device's largest supported text/display settings. Every drag action has Move up/Move down controls. No critical action requires long press, swipe, color discrimination or timed touch.

Essential controls remain visible above the keyboard and system gesture area. Focus ring states and hardware keyboard navigation are explicit. Language strings use resources; English ships first, architecture supports Kannada later without clipping assumptions. Decimal separators, dates and time formats respect locale while stored values use stable formats.

## 14. Design handoff and review checklist

Deliver component previews for light/dark/high contrast and both palettes; normal/large text; realistic empty, populated, unavailable and failure states. Demo data is confined to previews or a separately marked Demo mode with no path into the real database or exports.

Required screen reviews: first run, Today normal/minimum/paused, habit editor, timeline detail, Health empty/permission/active, strength sets, Focus setup/running/paused/partial/completed, dim view, Pocket Reset, voluntary interruption, Review sparse/full, Safety locked/error, Settings, Markdown preview, backup and restore failure.

Acceptance requires visual comparison with S3/S4/S2/S5/S6 for structure and with E for tone—not pixel copying at the expense of legibility. Review the final UI on the Xiaomi 14 and a narrow emulator. Measure actual contrast, tap targets, text clipping and scrolling before calling accessibility verified. No screenshot alone proves sensor activity, encryption, reliable alarms or app blocking.
