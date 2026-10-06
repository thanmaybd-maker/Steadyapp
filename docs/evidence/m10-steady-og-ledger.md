# Steady OG integration verification ledger

Updated 7 October 2026. The earlier ledger declared integration without row-specific evidence and referenced nonexistent symbols. Those claims are withdrawn. Compilation alone is not feature acceptance. This ledger starts from the audited 76-row matrix; statuses advance only with implementation, visible wiring and applicable passing checks.

The latest owner direction includes OG cards, water/habit animations and graph interactions in Kinetic/Daybook themes, with theme-aware text colors. Optional Gemini BYOK remains requested; it is not rejected merely because the core is offline.

| ID | Capability | Intended destination | Verification status |
|---|---|---|---|
| OG-01 | Create/edit/remove categorized prioritized tasks with duration/time | ui/ExpandedToday.kt;data/ExpandedRepository.kt;data/ExpandedEntities.kt | Pending source/wiring/acceptance audit |
| OG-02 | Complete and undo task; daily completion summary | ui/ExpandedToday.kt;data/ExpandedRepository.kt | Pending source/wiring/acceptance audit |
| OG-03 | Morning/afternoon/evening proposal windows with duration/task/primer | ui/ExpandedToday.kt;ui/TaskTimeline.kt;domain | Pending source/wiring/acceptance audit |
| OG-04 | Adopt block into Today | data/ExpandedRepository.kt;data/ExpandedEntities.kt;ui/ExpandedToday.kt | Pending source/wiring/acceptance audit |
| OG-05 | Per-block reminder toggle/cancel and lead time | platform/RoutineReminders.kt;platform/ActivityAlarmAdapter.kt | Pending source/wiring/acceptance audit |
| OG-06 | Global focus reminder preference | data/ExpandedProfile;ui/ReminderSettingsEditor.kt;platform/RoutineReminders.kt | Pending source/wiring/acceptance audit |
| OG-07 | Notification permission request and state | ui/ExpandedFocus.kt;platform/NotificationAdapter.kt | Pending source/wiring/acceptance audit |
| OG-08 | Immediate test reminder | platform/NotificationAdapter.kt;ui/ReminderSettingsEditor.kt | Pending source/wiring/acceptance audit |
| OG-09 | Reminder content tap opens appropriate destination | MainActivity.kt;ui/ExpandedNavigation.kt;platform | Pending source/wiring/acceptance audit |
| OG-10 | Add/delete habit; category/color/schedule intent | data/HabitDefinition;data/HabitVersion;ui/ExpandedToday.kt | Pending source/wiring/acceptance audit |
| OG-11 | Habit completion and undo | data/ExpandedRepository.kt;domain/ActivityRules.kt:HabitRules | Pending source/wiring/acceptance audit |
| OG-12 | Daily habit momentum summary | ui/ExpandedToday.kt;ui/DashboardMetrics.kt | Pending source/wiring/acceptance audit |
| OG-13 | Current/best consistency summary | domain/HabitRules;ui/ExpandedToday.kt;ui/ExpandedReview.kt | Pending source/wiring/acceptance audit |
| OG-14 | Seven-day visual history | ui/ExpandedReview.kt;data/HabitOccurrence | Pending source/wiring/acceptance audit |
| OG-15 | Water/focus/pause/movement mini-summary matrix | ui/ExpandedToday.kt;ui/DashboardMetrics.kt;ui/ExpandedReview.kt | Pending source/wiring/acceptance audit |
| OG-16 | Quick serving addition and target progress | ui/ExpandedHealth.kt;data/WaterLog;ui/DashboardSettingsEditor.kt | Pending source/wiring/acceptance audit |
| OG-17 | Three animated concentric progress channels and legends | ui/ExpandedComponents.kt:MetricRing;ui/DashboardMetrics.kt;ui/ExpandedFocus.kt:FocusRings | Pending source/wiring/acceptance audit |
| OG-18 | Calories/steps/active-time channels and goal values | data/ActivityObservation;data/EnergyEstimate;data/ExpandedProfile | Pending source/wiring/acceptance audit |
| OG-19 | Live sensor/elevation/calorie-rate ticker | platform/PlatformSensors.kt;ui/ExpandedHealthWidgets.kt | Pending source/wiring/acceptance audit |
| OG-20 | Day curve and colored event markers | ui/TaskTimeline.kt;ui/ExpandedHealthWidgets.kt | Pending source/wiring/acceptance audit |
| OG-21 | Current-time pulsing beacon | ui/TaskTimeline.kt;domain/LogicalDayPolicy.kt | Pending source/wiring/acceptance audit |
| OG-22 | Study/workout/reflection schedule nodes | ui/TaskTimeline.kt;ui/ExpandedToday.kt | Pending source/wiring/acceptance audit |
| OG-23 | Four study/rest presets and subject entry | ui/ExpandedFocus.kt;data/ActivityRepository.kt | Pending source/wiring/acceptance audit |
| OG-24 | Start/pause/resume/reset/mode switch | data/ActivityRepository.kt;domain/ActivityRules.kt;platform/ActivityAlarmAdapter.kt | Pending source/wiring/acceptance audit |
| OG-25 | Skip next phase and save completed/partial session | data/ActivityRepository.kt;ui/ExpandedFocus.kt | Pending source/wiring/acceptance audit |
| OG-26 | Study-to-break and break-to-study transition | data/ActivitySession;data/ActivitySegment;ui/ExpandedFocus.kt | Pending source/wiring/acceptance audit |
| OG-27 | Timer ring/countdown/session count/recent session list | ui/ExpandedFocus.kt;ui/ExpandedReview.kt | Pending source/wiring/acceptance audit |
| OG-28 | Dim screen exit and comfortable breathing | ui/ExpandedFocus.kt;ui/ExpandedToday.kt:PocketReset | Pending source/wiring/acceptance audit |
| OG-29 | Adjustable voluntary practice duration and countdown | ui/ExpandedFocus.kt:VoluntaryPause;data/InterruptionEvent | Pending source/wiring/acceptance audit |
| OG-30 | App-blocking and time-saved claims | platform/UsageInterceptor.kt;ui/ExpandedUsageInsights.kt | Pending source/wiring/acceptance audit |
| OG-31 | Forest/rain/brown/white/modulated-tone/campfire generation | platform/AudioSoundscapeEngine.kt | Pending source/wiring/acceptance audit |
| OG-32 | Play/stop/release and real playing state | platform/AudioSoundscapeEngine.kt;di/AppContainer.kt | Pending source/wiring/acceptance audit |
| OG-33 | Sound selection during playback | platform/AudioSoundscapeEngine.kt;ui/ExpandedFocus.kt:AmbientSoundscape | Pending source/wiring/acceptance audit |
| OG-34 | Independent0..1 volume | platform/AudioSoundscapeEngine.kt;ui/ExpandedFocus.kt | Pending source/wiring/acceptance audit |
| OG-35 | Opt-in timer-linked auto-play/pause | data/ExpandedProfile or encrypted preferences;ui/ExpandedFocus.kt | Pending source/wiring/acceptance audit |
| OG-36 | Three nature scene choices and sound association | ui/ExpandedFocus.kt;res/drawable/img_*_portal.xml | Pending source/wiring/acceptance audit |
| OG-37 | Full-screen portal play/volume/close | ui/ExpandedFocus.kt;ui/ExpandedComponents.kt:DialogSurface | Pending source/wiring/acceptance audit |
| OG-38 | Animated sound bars and breathing decoration | ui/ExpandedFocus.kt;ui/SteadyTheme.kt | Pending source/wiring/acceptance audit |
| OG-39 | Image-generation quota dialog | ui/ExpandedFocus.kt;ui/LicensesScreen.kt | Pending source/wiring/acceptance audit |
| OG-40 | Five donor workout modes | ui/ExpandedHealth.kt;ui/WorkoutControls.kt | Pending source/wiring/acceptance audit |
| OG-41 | Workout start/cancel/finish and elapsed display | data/ActivityRepository.kt;ui/ExpandedHealth.kt | Pending source/wiring/acceptance audit |
| OG-42 | Manual rep increment and notes | ui/ExpandedHealth.kt:SetEditor;data/ExerciseSet | Pending source/wiring/acceptance audit |
| OG-43 | Workout history/delete and totals | ui/ExpandedReview.kt;data/ActivityRepository.kt | Pending source/wiring/acceptance audit |
| OG-44 | MET activity energy estimate | data/EnergyEstimate;domain;ui/ExpandedHealth.kt | Pending source/wiring/acceptance audit |
| OG-45 | Steps/cadence/sensor interpretation | platform/PlatformSensors.kt;data/ActivityObservation | Pending source/wiring/acceptance audit |
| OG-46 | Custom food idea create/detail/favorite/delete | data/FoodIdeaRecord;ui/ExpandedHealth.kt:FoodEditor;ui/FoodIdeasSheet.kt | Pending source/wiring/acceptance audit |
| OG-47 | Prep-time choices and pantry/custom ingredients | ui/FoodIdeasSheet.kt;ui/ExpandedHealth.kt;connected proposal editor | Pending source/wiring/acceptance audit |
| OG-48 | Meal type and chosen context | ui/ExpandedHealth.kt;connected meal proposal editor | Pending source/wiring/acceptance audit |
| OG-49 | Search recipe ideas with Google grounding | Connected provider interface;structured recipe proposal | Pending source/wiring/acceptance audit |
| OG-50 | Constrained generated meal result | Connected provider;FoodIdeaRecord;typed proposal | Pending source/wiring/acceptance audit |
| OG-51 | Save generated/searched recipe into ideas/favorites | data/ExpandedRepository.kt;FoodIdeaRecord | Pending source/wiring/acceptance audit |
| OG-52 | Schedule meal as Food task/break | data/PlanItem;ui/ExpandedToday.kt;ui/TaskTimeline.kt | Pending source/wiring/acceptance audit |
| OG-53 | Selected day/recent dates/past review navigation | ui/ExpandedReview.kt;domain/LogicalDayPolicy.kt | Pending source/wiring/acceptance audit |
| OG-54 | Paired daily focus/rest bars and minute axes | ui/ExpandedReview.kt;native chart or audited offline renderer | Pending source/wiring/acceptance audit |
| OG-55 | Totals and focus ratio | PeriodSnapshot;ActivitySegment;domain aggregation | Pending source/wiring/acceptance audit |
| OG-56 | Tap day bar to inspect data | ui/ExpandedReview.kt supporting records | Pending source/wiring/acceptance audit |
| OG-57 | Optional mood/energy/highlight/obstacle/tomorrow reflection | data/Reflection;ui/ExpandedReview.kt | Pending source/wiring/acceptance audit |
| OG-58 | Public country help directory and selection | ui/PublicSafetyPanel.kt;platform/BootstrapStore.kt;domain/SafetyModels.kt | Pending source/wiring/acceptance audit |
| OG-59 | Private extra notes with category/create/edit/delete/pin | data/PrivateSafetyRepository.kt;ui/SafetyScreen.kt;private schema | Pending source/wiring/acceptance audit |
| OG-60 | On-screen privacy mask | ui/SafetyScreen.kt;MainActivity.kt secure/auth contract | Pending source/wiring/acceptance audit |
| OG-61 | Trusted/campus contacts editor | data/PrivateSafetyRepository.kt;domain/SupportContact;ui/SafetyScreen.kt | Pending source/wiring/acceptance audit |
| OG-62 | Dial and SMS composer | platform/DialerAdapter.kt;ui/SafetyScreen.kt | Pending source/wiring/acceptance audit |
| OG-63 | Sensory grounding sequence | ui/SafetyScreen.kt;ui/ExpandedToday.kt:PocketReset | Pending source/wiring/acceptance audit |
| OG-64 | Record count summary | ui/ExpandedSettings.kt;data/ExpandedRepository.kt | Pending source/wiring/acceptance audit |
| OG-65 | PIN protection intent | ui/AccessViewModel.kt;MainActivity.kt;platform/BootstrapStore.kt | Pending source/wiring/acceptance audit |
| OG-66 | Markdown preview/copy/share intent | data/ExpandedMarkdown.kt;domain/MarkdownExport.kt;ui/SettingsFilesScreen.kt | Pending source/wiring/acceptance audit |
| OG-67 | Portable backup and restore intent | domain/BackupService.kt;data/PortableArchive.kt;data/RecoveryRepository.kt | Pending source/wiring/acceptance audit |
| OG-68 | Legacy OG organiser-file interoperability | platform/DocumentAdapter.kt;data/RecoveryRepository.kt;legacy parser if needed | Pending source/wiring/acceptance audit |
| OG-69 | Fast task decomposition and selected insertion | Gemini BYOK provider;typed task proposal;PlanItem | Pending source/wiring/acceptance audit |
| OG-70 | Schedule personalization | Gemini BYOK provider;typed block proposals;existing scheduler | Pending source/wiring/acceptance audit |
| OG-71 | Selected daily/weekly reflection rewrite | Gemini BYOK provider;Reflection;ui/ExpandedReview.kt | Pending source/wiring/acceptance audit |
| OG-72 | Provider transport/key/errors/grounding | Connected variant source/manifest;provider interface;protected key storage | Pending source/wiring/acceptance audit |
| OG-73 | Database/DAOs/Flow storage intent | data/SteadyDatabase.kt;data/ExpandedDao.kt;di/AppContainer.kt | Pending source/wiring/acceptance audit |
| OG-74 | Lifecycle navigation/ViewModel ownership | MainActivity.kt;ui/ExpandedNavigation.kt;existing factories | Pending source/wiring/acceptance audit |
| OG-75 | Manifest/build/signing/assets | app/build.gradle.kts;gradle/libs.versions.toml;AndroidManifest.xml;assets/licenses | Pending source/wiring/acceptance audit |
| OG-76 | Tests and complete parity evidence | Existing isolatedQA tests;docs/evidence;mergeledger | Pending source/wiring/acceptance audit |
