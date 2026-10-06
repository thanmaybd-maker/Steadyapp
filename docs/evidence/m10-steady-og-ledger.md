# Steady OG integration verification ledger

Updated 7 October 2026. The earlier blanket integration claims are withdrawn. These statuses come from the 76-row coverage CSV and actual checkpoints. Compilation is not device/UI acceptance. No whole-project completion is claimed.

The latest direction includes OG rich cards, water/habit animations and graphs in Kinetic/Daybook themes, with theme-aware text. Gemini BYOK remains requested in a separate opt-in connected implementation.

Detailed source mappings, dependencies, persistence, file scope, tests and exact blockers: [coverage CSV](m10-steady-og-ledger.csv). Latest evidence: [private notes](m10-private-notes.md), [legacy import](m10-legacy-import.md), [daily Review and drafts](m10-review-drafts.md). Earlier theme/device acceptance remains limited to its recorded checkpoints.

| ID | Capability | Destination | Verification status |
|---|---|---|---|
| OG-01 | Create/edit/remove categorized prioritized tasks with duration/time | ui/ExpandedToday.kt;data/ExpandedRepository.kt;data/ExpandedEntities.kt | Pending acceptance audit |
| OG-02 | Complete and undo task; daily completion summary | ui/ExpandedToday.kt;data/ExpandedRepository.kt | Pending acceptance audit |
| OG-03 | Morning/afternoon/evening proposal windows with duration/task/primer | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-04 | Adopt block into Today | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-05 | Per-block reminder toggle/cancel and lead time | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-06 | Global focus reminder preference | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-07 | Notification permission request and state | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-08 | Immediate test reminder | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-09 | Reminder content tap opens appropriate destination | StudyBlockPlanner; StudyBlockRules; ExpandedRepository.saveStudyBlock/adoptStudyBlock; RoutineReminders; AlarmCueStore; NotificationAdapter; MainActivity/ExpandedNavigation | Implemented; host and persistence checks pass; UI/platform acceptance pending |
| OG-10 | Add/delete habit; category/color/schedule intent | data/HabitDefinition;data/HabitVersion;ui/ExpandedToday.kt | Pending acceptance audit |
| OG-11 | Habit completion and undo | data/ExpandedRepository.kt;domain/ActivityRules.kt:HabitRules | Pending acceptance audit |
| OG-12 | Daily habit momentum summary | ui/ExpandedToday.kt;ui/DashboardMetrics.kt | Pending acceptance audit |
| OG-13 | Current/best consistency summary | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-14 | Seven-day visual history | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-15 | Water/focus/pause/movement mini-summary matrix | RecordedMiniMatrix; ExpandedToday; focusRestRows; activityMillis; HistoryEditor | Implemented; unlocked UI/visual acceptance pending |
| OG-16 | Quick serving addition and target progress | ExpandedHealth; AnimatedWaterFill; WaterEditor; ExpandedRepository.saveWater/deleteWater | Save/undo verified in both themes; full row audit pending |
| OG-17 | Three animated concentric progress channels and legends | RichProgress.ConcentricSummary; DashboardMetrics; ExpandedFocus.FocusRings | Dashboard implemented and prior QA passed; Focus/full row checks pending |
| OG-18 | Calories/steps/active-time channels and goal values | data/ActivityObservation;data/EnergyEstimate;data/ExpandedProfile | Pending acceptance audit |
| OG-19 | Live sensor/elevation/calorie-rate ticker | platform/PlatformSensors.kt;ui/ExpandedHealthWidgets.kt | Pending acceptance audit |
| OG-20 | Day curve and colored event markers | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-21 | Current-time pulsing beacon | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-22 | Study/workout/reflection schedule nodes | ui/TaskTimeline.kt;ui/ExpandedToday.kt | Pending acceptance audit |
| OG-23 | Four study/rest presets and subject entry | ui/ExpandedFocus.kt;data/ActivityRepository.kt | Pending acceptance audit |
| OG-24 | Start/pause/resume/reset/mode switch | data/ActivityRepository.kt;domain/ActivityRules.kt;platform/ActivityAlarmAdapter.kt | Pending acceptance audit |
| OG-25 | Skip next phase and save completed/partial session | data/ActivityRepository.kt;ui/ExpandedFocus.kt | Pending acceptance audit |
| OG-26 | Study-to-break and break-to-study transition | data/ActivitySession;data/ActivitySegment;ui/ExpandedFocus.kt | Pending acceptance audit |
| OG-27 | Timer ring/countdown/session count/recent session list | ui/ExpandedFocus.kt;ui/ExpandedReview.kt | Pending acceptance audit |
| OG-28 | Dim screen exit and comfortable breathing | ui/ExpandedFocus.kt;ui/ExpandedToday.kt:PocketReset | Pending acceptance audit |
| OG-29 | Adjustable voluntary practice duration and countdown | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-30 | App-blocking and time-saved claims | UsageDurationRules; UsageInterceptor; ExpandedUsageInsights; ExpandedSettings | Claims corrected; real Usage Access wired; cross-app interception unavailable |
| OG-31 | Forest/rain/brown/white/modulated-tone/campfire generation | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-32 | Play/stop/release and real playing state | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-33 | Sound selection during playback | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-34 | Independent0..1 volume | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-35 | Opt-in timer-linked auto-play/pause | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-36 | Three nature scene choices and sound association | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-37 | Full-screen portal play/volume/close | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-38 | Animated sound bars and breathing decoration | PcmSoundGenerator; AudioSoundscapeEngine; AmbientAudioCard; StudySetsCard; LiveScratchpad; VoluntaryPause | Implemented; host generation checks pass; new device journeys pending |
| OG-39 | Image-generation quota dialog | AmbientAudioCard; LicensesScreen; steady-og-source.txt | Fabricated quota omitted; actual offline scene assets retained |
| OG-40 | Five donor workout modes | ui/ExpandedHealth.kt;ui/WorkoutControls.kt | Pending acceptance audit |
| OG-41 | Workout start/cancel/finish and elapsed display | data/ActivityRepository.kt;ui/ExpandedHealth.kt | Pending acceptance audit |
| OG-42 | Manual rep increment and notes | ui/ExpandedHealth.kt:SetEditor;data/ExerciseSet | Pending acceptance audit |
| OG-43 | Workout history/delete and totals | ui/ExpandedReview.kt;data/ActivityRepository.kt | Pending acceptance audit |
| OG-44 | MET activity energy estimate | data/EnergyEstimate;domain;ui/ExpandedHealth.kt | Pending acceptance audit |
| OG-45 | Steps/cadence/sensor interpretation | platform/PlatformSensors.kt;data/ActivityObservation | Pending acceptance audit |
| OG-46 | Custom food idea create/detail/favorite/delete | FoodEditor; RecipeContextSummary; MealPlanEditor; FoodPlanningRules; ExpandedRepository.saveRecipe/adoptMeal; TaskEditor | Implemented manual workflow; host and encrypted storage checks pass; UI/connected portions pending |
| OG-47 | Prep-time choices and pantry/custom ingredients | FoodEditor; RecipeContextSummary; MealPlanEditor; FoodPlanningRules; ExpandedRepository.saveRecipe/adoptMeal; TaskEditor | Implemented manual workflow; host and encrypted storage checks pass; UI/connected portions pending |
| OG-48 | Meal type and chosen context | FoodEditor; RecipeContextSummary; MealPlanEditor; FoodPlanningRules; ExpandedRepository.saveRecipe/adoptMeal; TaskEditor | Implemented manual workflow; host and encrypted storage checks pass; UI/connected portions pending |
| OG-49 | Search recipe ideas with Google grounding | Connected provider interface;structured recipe proposal | Connected implementation pending |
| OG-50 | Constrained generated meal result | Connected provider;FoodIdeaRecord;typed proposal | Connected implementation pending |
| OG-51 | Save generated/searched recipe into ideas/favorites | data/ExpandedRepository.kt;FoodIdeaRecord | Connected implementation pending |
| OG-52 | Schedule meal as Food task/break | FoodEditor; RecipeContextSummary; MealPlanEditor; FoodPlanningRules; ExpandedRepository.saveRecipe/adoptMeal; TaskEditor | Implemented manual workflow; host and encrypted storage checks pass; UI/connected portions pending |
| OG-53 | Selected day/recent dates/past review navigation | ui/ExpandedReview.kt;domain/LogicalDayPolicy.kt | Pending acceptance audit |
| OG-54 | Paired daily focus/rest bars and minute axes | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-55 | Totals and focus ratio | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-56 | Tap day bar to inspect data | ReviewChartRules; FocusRestChart; HabitWeekHistory; TaskTimeline; ExpandedReview | Implemented; deterministic host checks pass; new UI checks pending |
| OG-57 | Optional mood/energy/highlight/obstacle/tomorrow reflection | ui/ExpandedReview daily/week/month;ExpandedViewModel.openDraft/retryDraft;domain/ReflectionDraftRules.rating;data/ExpandedRepository.saveReflection | Implemented; host/native verified; UI acceptance pending |
| OG-58 | Public country help directory and selection | ui/PublicSafetyPanel.kt;platform/BootstrapStore.kt;domain/SafetyModels.kt | Pending acceptance audit |
| OG-59 | Private extra notes with category/create/edit/delete/pin | data/PrivateSafetyNotes.kt;PrivateSafetyRepository.notes/saveNote/saveNoteDraft/pinNote/deleteNote;ui/SafetyNotesPanel.kt;SafetyNotesViewModel.kt | Implemented; host and storage/platform verified; UI acceptance pending |
| OG-60 | On-screen privacy mask | ui/SafetyScreen.kt masked; lifecycle ON_STOP; SafetyNotesPanel | Implemented; host and storage/platform verified; UI acceptance pending |
| OG-61 | Trusted/campus contacts editor | data/PrivateSafetyRepository.kt;domain/SupportContact;ui/SafetyScreen.kt | Pending acceptance audit |
| OG-62 | Dial and SMS composer | platform/DialerAdapter.openSmsComposer/openDialer;ui/SafetyScreen.kt | Implemented; host and storage/platform verified; UI acceptance pending |
| OG-63 | Sensory grounding sequence | ui/SensoryGrounding.kt;PublicSafetyPanel.kt | Implemented; host and storage/platform verified; UI acceptance pending |
| OG-64 | Record count summary | ui/ExpandedSettings.kt;data/ExpandedRepository.kt | Pending acceptance audit |
| OG-65 | PIN protection intent | ui/AccessViewModel.kt;MainActivity.kt;platform/BootstrapStore.kt | Pending acceptance audit |
| OG-66 | Markdown preview/copy/share intent | data/ExpandedMarkdown.kt;domain/MarkdownExport.kt;ui/SettingsFilesScreen.kt | Pending acceptance audit |
| OG-67 | Portable backup and restore intent | domain/BackupService.kt;data/PortableArchive.kt;data/RecoveryRepository.kt | Pending acceptance audit |
| OG-68 | Legacy OG organiser-file interoperability | data/LegacyOgImport.kt:LegacyOgCodec/LegacyOgImporter;RecoveryRepository.importLegacy;platform/DocumentAdapter.readLegacyJsonFromUri;ui/RecoveryViewModel.prepareLegacy/confirmLegacy;SettingsFilesScreen | Implemented; host/native verified; unlocked file journey pending |
| OG-69 | Fast task decomposition and selected insertion | Gemini BYOK provider;typed task proposal;PlanItem | Connected implementation pending |
| OG-70 | Schedule personalization | Gemini BYOK provider;typed block proposals;existing scheduler | Connected implementation pending |
| OG-71 | Selected daily/weekly reflection rewrite | Gemini BYOK provider;Reflection;ui/ExpandedReview.kt | Connected implementation pending |
| OG-72 | Provider transport/key/errors/grounding | Connected variant source/manifest;provider interface;protected key storage | Connected implementation pending |
| OG-73 | Database/DAOs/Flow storage intent | data/SteadyDatabase.kt;data/ExpandedDao.kt;di/AppContainer.kt | Pending acceptance audit |
| OG-74 | Lifecycle navigation/ViewModel ownership | MainActivity.kt;ui/ExpandedNavigation.kt;existing factories | Pending acceptance audit |
| OG-75 | Manifest/build/signing/assets | app/build.gradle.kts;gradle/libs.versions.toml;AndroidManifest.xml;assets/licenses | Pending acceptance audit |
| OG-76 | Tests and complete parity evidence | Existing isolatedQA tests;docs/evidence;mergeledger | Pending acceptance audit |
