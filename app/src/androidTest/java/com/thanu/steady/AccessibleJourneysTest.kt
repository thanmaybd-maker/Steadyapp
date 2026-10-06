package com.thanu.steady

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityClock
import com.thanu.steady.platform.*
import com.thanu.steady.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
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
            ActivityAlarmAdapter(context),NotificationAdapter(context),BootstrapStore(context),{ false })
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
            throw AssertionError("Save bounds root=${saveNode.boundsInRoot}, window=${saveNode.boundsInWindow}, screen=${compose.activity.resources.displayMetrics.heightPixels}; parent=${saveNode.parent?.boundsInRoot}",failure)
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
}
