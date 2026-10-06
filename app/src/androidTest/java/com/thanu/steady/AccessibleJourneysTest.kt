package com.thanu.steady

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityClock
import com.thanu.steady.platform.*
import com.thanu.steady.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.UUID

class AccessibleJourneysTest {
    @get:Rule val compose = createAndroidComposeRule<UiHarnessActivity>()
    private lateinit var db: SteadyDatabase
    private lateinit var model: ExpandedViewModel
    private lateinit var repository: ExpandedRepository
    private var store = ViewModelStore()
    private lateinit var name: String
    private val clock = Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"),ZoneOffset.UTC)
    private fun label(id: Int) = compose.activity.getString(id)
    @Before fun prepare() {
        val context = compose.activity
        System.loadLibrary("sqlcipher"); name = "ui-synthetic-${UUID.randomUUID()}.db"
        db = Room.databaseBuilder(context,SteadyDatabase::class.java,name)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes))).build()
        val preferences = PreferencesRepository { db }
        repository = ExpandedRepository({ db },clock,preferences)
        runBlocking(Dispatchers.IO) { repository.saveProfile(ExpandedProfile(onboarded = true)); repository.prepareDay(LocalDate.parse("2026-01-05")) }
        model = ExpandedViewModel(repository,ActivityRepository({ db },{ ActivityClock(clock.millis(),1000,1) },preferences),preferences,
            ActivityAlarmAdapter(context),NotificationAdapter(context),BootstrapStore(context),PlatformSensors(context),AudioSoundscapeEngine(context),{ false })
        store.put("synthetic",model)
    }
    @After fun cleanup() {
        compose.runOnIdle { store.clear() }
        db.close(); compose.activity.deleteDatabase(name)
    }
    private fun today(large: Boolean = false) {
        compose.setContent {
            SteadyTheme(ExpandedProfile(textScale = if(large) 2f else 1f)) {
                val state by model.state.collectAsState()
                if(state.period != null) ExpandedToday(model,state,{}, {}, {})
            }
        }
        compose.waitUntil(10000) { model.state.value.period != null }
    }
    private fun button(id: Int) = compose.onNode(hasText(label(id)) and hasClickAction())
    private fun field(id: Int) = compose.onNode(hasText(label(id)) and hasSetTextAction())
    @Test fun invalidTaskPreservesDraftThenCompletionUndoAndReschedulePersist() {
        today()
        button(R.string.add_task).performScrollTo().performClick()
        field(R.string.task_title).performTextInput("Synthetic journey task")
        field(R.string.duration_minutes_optional).performTextInput("invalid")
        button(R.string.save_action).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.error == R.string.action_failed && !model.state.value.busy }
        compose.onAllNodesWithText(label(R.string.action_failed)).onFirst().assertExists()
        field(R.string.task_title).assertTextContains("Synthetic journey task")
        button(R.string.close_keep_draft).performScrollTo().performClick()
        button(R.string.add_task).performScrollTo().performClick()
        field(R.string.task_title).assertTextContains("Synthetic journey task")
        field(R.string.duration_minutes_optional).performTextReplacement("20")
        button(R.string.save_action).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.period?.tasks?.size == 1 && !model.state.value.busy }
        button(R.string.complete_task).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.period?.tasks?.firstOrNull()?.state == "COMPLETED" && !model.state.value.busy }
        button(R.string.undo_complete).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.period?.tasks?.firstOrNull()?.state == "PENDING" && !model.state.value.busy }
        button(R.string.edit_reschedule).performScrollTo().performClick()
        field(R.string.logical_date).performTextReplacement("2026-01-06")
        button(R.string.save_action).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.period?.tasks?.isEmpty() == true && !model.state.value.busy }
        runBlocking(Dispatchers.IO) {
            val stored = repository.snapshot(LocalDate.parse("2026-01-06"),LocalDate.parse("2026-01-06")).tasks.single()
            assertEquals("Synthetic journey task",stored.title); assertEquals("PENDING",stored.state); assertEquals(1200L,stored.plannedSeconds)
        }
    }
    @Test fun doubleTextScaleKeepsLabelledEssentialEditorControlsReachable() {
        today(true)
        button(R.string.add_task).performScrollTo().performClick()
        button(R.string.safety_action).performScrollTo().assertIsDisplayed()
        field(R.string.task_title).performScrollTo().assertIsDisplayed()
        button(R.string.save_action).performScrollTo()
        compose.waitForIdle()
        val saveNode = button(R.string.save_action).fetchSemanticsNode()
        try { button(R.string.save_action).assertIsDisplayed() } catch (failure: AssertionError) {
            val frame = android.graphics.Rect()
            compose.activity.window.decorView.getWindowVisibleDisplayFrame(frame)
            val roots = compose.onAllNodes(isRoot(),useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInWindow }
            throw AssertionError("Save root=${saveNode.boundsInRoot}, unclipped=${button(R.string.save_action).getUnclippedBoundsInRoot()}, window=${saveNode.boundsInWindow}; frame=$frame; roots=$roots",failure)
        }
        val height = button(R.string.save_action).fetchSemanticsNode().boundsInRoot.height
        assertTrue(height >= 56 * compose.activity.resources.displayMetrics.density - 1)
        button(R.string.close_keep_draft).performScrollTo().assertIsDisplayed()
    }
    @Test fun publicHelpRendersWithoutPersonalRepositoryOrAuthentication() {
        compose.setContent { SteadyTheme(ExpandedProfile(textScale = 2f,highContrast = true)) { ExpandedPage { PublicSafetyPanel("IN") } } }
        button(R.string.dial_telemanas).performScrollTo().assertIsDisplayed()
        button(R.string.dial_emergency).performScrollTo().assertIsDisplayed()
        // Do not tap dial actions or capture Safety screenshots. Neither personal store is supplied.
    }
    @Test fun keyboardAndDoubleTextKeepSaveReachable() {
        today(true)
        button(R.string.add_task).performScrollTo().performClick()
        field(R.string.task_title).performScrollTo().performClick().performTextInput("Synthetic keyboard draft")
        button(R.string.save_action).performScrollTo().assertIsDisplayed()
        field(R.string.task_title).performScrollTo().assertTextContains("Synthetic keyboard draft")
        button(R.string.close_keep_draft).performScrollTo().assertIsDisplayed()
    }
    @Test fun customizedSummariesAndShortcutsPersistWithoutDuplicateMetrics() {
        compose.setContent { SteadyTheme {
            val state by model.state.collectAsState()
            if(state.period != null) DashboardSettingsEditor(model,state,{}, {})
        } }
        compose.waitUntil(10000) { model.state.value.period != null }
        field(R.string.water_quick_quantities).performScrollTo().performTextReplacement("125,375")
        button(R.string.save_action).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.period?.profile?.waterQuickMl == "125,375" && !model.state.value.busy }
        runBlocking(Dispatchers.IO) { assertEquals("125,375",repository.profile().waterQuickMl) }
    }
    @Test fun foodChoicesRetainDraftAndSchedulingCreatesOnlyOneFoodPlanAtDoubleText() {
        var stage by mutableStateOf("recipe")
        compose.setContent {
            SteadyTheme(ExpandedProfile(palette="DAYBOOK",theme="DARK",textScale=2f,reducedMotion=true)) {
                val state by model.state.collectAsState()
                if(state.period != null) {
                    if(stage == "recipe") FoodEditor(model,state,null,{}) { stage="plan" }
                    else state.period!!.foods.singleOrNull()?.let { recipe -> MealPlanEditor(model,state,recipe,{}) { stage="closed" } }
                }
            }
        }
        compose.waitUntil(10000) { model.state.value.period != null }
        button(R.string.pantry_rice).performScrollTo().performClick()
        compose.onNode(hasText(compose.activity.getString(R.string.preparation_minutes,10)) and hasClickAction()).performScrollTo().performClick()
        button(R.string.meal_lunch).performScrollTo().performClick()
        field(R.string.meal_context).performScrollTo().performTextInput("Synthetic context")
        field(R.string.food_instructions).performScrollTo().performTextInput("Synthetic preparation")
        button(R.string.save_action).performScrollTo().performClick()
        compose.waitUntil(10000) { model.state.value.error == R.string.action_failed }
        field(R.string.meal_context).performScrollTo().assertTextContains("Synthetic context")
        field(R.string.food_name).performScrollTo().performTextInput("Synthetic lunch")
        button(R.string.save_action).performScrollTo().performClick()
        compose.waitUntil(10000) { stage == "plan" && model.state.value.period?.foods?.size == 1 }
        val recipe = runBlocking(Dispatchers.IO) { db.expandedDao().foods().single() }
        assertEquals(10,recipe.prepMinutes)
        assertTrue(recipe.ingredients.contains(label(R.string.pantry_rice)))
        assertEquals(com.thanu.steady.domain.RecipeContext("LUNCH","Synthetic context"),runBlocking(Dispatchers.IO) { repository.recipeContext(recipe.id) })
        field(R.string.time_optional).performScrollTo().performTextInput("13:00")
        button(R.string.add_food_plan).performScrollTo().performClick()
        compose.waitUntil(10000) { stage == "closed" && model.state.value.period?.tasks?.size == 1 }
        val plan = runBlocking(Dispatchers.IO) { db.expandedDao().tasks("2026-01-05","2026-01-05").single() }
        assertEquals("FOOD",plan.category); assertEquals(780,plan.timeMinutes)
        assertTrue(runBlocking(Dispatchers.IO) { db.expandedDao().meals("2026-01-05","2026-01-05").isEmpty() })
    }

    @Test fun kineticWaterShortcutAndUndoChangeStoredServings() = waterJourney("KINETIC", "LIGHT", 1f)
    @Test fun daybookDarkDoubleTextKeepsWaterAndUndoReachable() = waterJourney("DAYBOOK", "DARK", 2f)

    @Test fun audioControlsPortalAndRecoveredPreferencesUseEncryptedRecords() {
        compose.setContent {
            val state by model.state.collectAsState()
            state.period?.let { SteadyTheme(ExpandedProfile(palette = "DAYBOOK", theme = "DARK", textScale = 2f, reducedMotion = true)) {
                ExpandedPage { AmbientSoundscape(model, state, {}) }
            } }
        }
        compose.waitUntil(10000) { model.state.value.period != null }
        val tone = compose.activity.getString(R.string.available_choice, label(R.string.sound_tone))
        compose.onNodeWithText(tone).performScrollTo().performClick()
        compose.onAllNodesWithContentDescription(label(R.string.audio_modulation_label)).onLast()
            .performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        compose.onNodeWithContentDescription(label(R.string.audio_volume_label))
            .performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(0.35f) }
        button(R.string.audio_portal).performScrollTo().performClick()
        button(R.string.safety_action).performScrollTo().assertIsDisplayed()
        val rain = compose.activity.getString(R.string.available_choice, label(R.string.audio_scene_rain))
        compose.onNodeWithText(rain).performScrollTo().performClick()
        button(R.string.close_action).performScrollTo().performClick()
        compose.waitUntil(10000) { model.ambientSaved.value && model.ambient.value.scene == "RAIN" }
        runBlocking(Dispatchers.IO) {
            val saved = Json.decodeFromString<AmbientPreferences>(repository.note("ambient:preferences")!!.text)
            assertEquals("ALPHA_BINAURAL", saved.sound); assertEquals(40.0, saved.modulation, 0.01)
            assertEquals(0.35f, saved.volume, 0.01f); assertEquals("RAIN", saved.scene)
            repository.saveNote(SessionNote("ambient:preferences", null, Json.encodeToString(
                AmbientPreferences(sound = "CAMPFIRE", volume = 0.1f, scene = "FOREST")), clock.millis()))
        }
        compose.runOnIdle { model.afterRecovery() }
        compose.waitUntil(10000) { model.ambient.value.sound == "CAMPFIRE" }
        assertFalse(model.audioSoundscapeEngine.isPlaying.value)
    }

    @Test fun voluntaryReturnNeedsNoTimerAndSavesReasonAndRealCounter() {
        var closed by mutableStateOf(false)
        compose.setContent {
            val state by model.state.collectAsState()
            SteadyTheme(ExpandedProfile(textScale = 2f, reducedMotion = true)) {
                if (state.period != null && !closed) VoluntaryPause(model, state, {}, { closed = true })
            }
        }
        compose.waitUntil(10000) { model.state.value.period != null }
        field(R.string.pause_reason).performScrollTo().performTextInput("Synthetic intention")
        button(R.string.pause_return).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10000) { closed && !model.state.value.busy }
        runBlocking(Dispatchers.IO) {
            assertEquals(1, repository.interruptionCount(LocalDate.parse("2026-01-05")))
            assertEquals("Synthetic intention", repository.snapshot(LocalDate.parse("2026-01-05"),
                LocalDate.parse("2026-01-05")).captures.single().text)
            val event = db.expandedDao().interruptions(clock.millis(), clock.millis()).single()
            assertEquals("RETURNED", event.outcome); assertEquals(0, event.pauseSeconds)
        }
    }

    @Test fun recordedFocusRestBarsSelectExactDayAndFollowHistoryCorrections() {
        val focusId = UUID.randomUUID().toString()
        val breakId = UUID.randomUUID().toString()
        runBlocking(Dispatchers.IO) {
            repository.saveProfile(ExpandedProfile(onboarded = true, palette = "DAYBOOK", theme = "DARK", textScale = 2f, reducedMotion = true))
            listOf(focusId to "FOCUS", breakId to "BREAK").forEach { (id, type) ->
                val start = clock.millis() + if(type == "BREAK") 120_000 else 0
                val millis = if(type == "FOCUS") 120_000L else 60_000L
                db.expandedDao().save(ActivitySession(id, type, type, "Synthetic $type", state = "STOPPED",
                    activeMillis = millis, started = start, ended = start + millis, zone = "UTC", boundary = 0, updated = start))
                db.expandedDao().save(ActivitySegment(UUID.randomUUID().toString(), id, start, start + millis, 0, millis, millis, "UTC", 0))
            }
        }
        compose.setContent {
            val state by model.state.collectAsState()
            state.period?.let { SteadyTheme(it.profile) { ExpandedReview(model, state, {}) } }
        }
        compose.waitUntil(10000) { model.state.value.period?.start == LocalDate.parse("2025-12-30") }
        val row = compose.activity.getString(R.string.focus_rest_day, "2026-01-05", 2.0, 1.0)
        compose.onNodeWithText(row).performScrollTo().assertIsDisplayed().performClick()
        compose.onAllNodes(hasText(label(R.string.supporting_records)) and hasClickAction()).onLast().performScrollTo().performClick()
        compose.onNodeWithText("Synthetic FOCUS").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.rest_day_minutes, 1.0)).performScrollTo().assertIsDisplayed()
        button(R.string.close_action).performScrollTo().performClick()
        val activity = ActivityRepository({ db }, { ActivityClock(clock.millis(), 1000, 1) }, PreferencesRepository { db })
        runBlocking(Dispatchers.IO) { activity.editHistory(focusId, 30_000, "Synthetic correction", null) }
        compose.waitUntil(10000) { model.state.value.period?.let { focusRestRows(it).last().focusMillis == 30_000L } == true }
        compose.onNodeWithText(compose.activity.getString(R.string.focus_rest_day, "2026-01-05", 0.5, 1.0)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.focus_rest_graph_description)).performScrollTo()
            .captureToImage().asAndroidBitmap().let { bitmap ->
                java.io.File(compose.activity.filesDir, "qa-focus-rest-daybook.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
        runBlocking(Dispatchers.IO) { activity.deleteHistory(breakId) }
        compose.waitUntil(10000) { model.state.value.period?.let { focusRestRows(it).last().restMillis == 0L } == true }
    }

    @Test fun sevenDayHabitHistoryDistinguishesMissingPausedPartialAndActualCompletion() {
        val habit = UUID.randomUUID().toString(); val version = UUID.randomUUID().toString()
        runBlocking(Dispatchers.IO) {
            db.expandedDao().save(HabitDefinition(habit, clock.millis()))
            db.expandedDao().save(HabitVersion(version, habit, "2025-12-30", "Synthetic dated habit", "CHECKBOX", "check", 1.0,
                anchorDay = "2025-12-30", created = clock.millis()))
            db.expandedDao().save(DaySettings("2025-12-31", "PAUSED", zone = "UTC", boundary = 0, updated = clock.millis()))
            listOf(Triple("2025-12-30", "COMPLETED", 1.0), Triple("2026-01-01", "SKIPPED", 0.0), Triple("2026-01-02", "PARTIAL", 0.5)).forEach { (day, status, quantity) ->
                db.expandedDao().save(HabitOccurrence(UUID.randomUUID().toString(), habit, version, day, status, quantity, updated = clock.millis()))
            }
            repository.prepareDay(LocalDate.parse("2026-01-05"))
        }
        today(true)
        compose.onNodeWithText(compose.activity.getString(R.string.habit_day_dot, "2026-01-03", label(R.string.missing)))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.habit_day_dot, "2025-12-31", label(R.string.not_scheduled)))
            .performScrollTo().assertIsDisplayed()
        val occurrence = runBlocking(Dispatchers.IO) { repository.snapshot(LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-05")).occurrences.single() }
        runBlocking(Dispatchers.IO) { repository.logHabit(occurrence.id, 1.0, UUID.randomUUID().toString()) }
        val completed = compose.activity.getString(R.string.habit_day_dot, "2026-01-05", label(R.string.completed))
        compose.waitUntil(10000) { compose.onAllNodesWithText(completed).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(completed).performScrollTo().assertIsDisplayed()
    }

    private fun waterJourney(palette: String, theme: String, textScale: Float) {
        runBlocking(Dispatchers.IO) { repository.saveProfile(ExpandedProfile(onboarded = true,
            palette = palette, theme = theme, textScale = textScale, reducedMotion = true,
            waterTargetMl = 1000, waterQuickMl = "250")) }
        compose.setContent {
            val state by model.state.collectAsState()
            state.period?.let { period -> SteadyTheme(period.profile) { ExpandedHealth(model, state, {}) } }
        }
        compose.waitUntil(10000) { model.state.value.period?.profile?.waterTargetMl == 1000 }
        val quickLabel = compose.activity.getString(R.string.quick_water, compose.activity.getString(R.string.water_serving, 250))
        compose.onNode(hasText(quickLabel) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10000) { model.state.value.period?.water?.sumOf { it.millilitres } == 250 && !model.state.value.busy }
        runBlocking(Dispatchers.IO) {
            assertEquals(250, repository.snapshot(LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-05")).water.sumOf { it.millilitres })
        }
        button(R.string.undo_water_log).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10000) { model.state.value.period?.water?.isEmpty() == true && !model.state.value.busy }
        runBlocking(Dispatchers.IO) {
            assertTrue(repository.snapshot(LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-05")).water.isEmpty())
        }
    }
}
