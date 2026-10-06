package com.thanu.steady

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.platform.BootstrapStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

/** Only the disposable QA package's synthetic default files are affected. No Safety screenshots. */
class PublicSafetyFailureTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun prepare() {
        check(context.packageName.endsWith(".qa"))
        runBlocking { BootstrapStore(context).clear() }
    }
    @After fun close() { scenario?.close(); runBlocking { BootstrapStore(context).clear() } }
    @Test fun organiserAndPrivateStoreFailureStillLeavePublicDialActionsVisible() {
        context.getDatabasePath("steady_encrypted.db").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1,2,3,4)) }
        context.getDatabasePath("steady_safety.db").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1,2,3,4)) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val error = context.getString(R.string.storage_unavailable)
        compose.waitUntil(10000) { compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(context.getString(R.string.safety_action)) and hasClickAction()).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText(context.getString(R.string.private_plan_load_failed)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(context.getString(R.string.dial_emergency)) and hasClickAction() and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.dial_telemanas)) and hasClickAction() and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
    }
    @Test fun authenticationCancellationKeepsPublicHelpReachable() {
        runBlocking { BootstrapStore(context).setLock(true) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10000) { compose.onAllNodesWithText(context.getString(R.string.unlock_app)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(context.getString(R.string.unlock_app)) and hasClickAction()).performScrollTo().performClick()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(200,5000)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNode(hasText(context.getString(R.string.dial_emergency)) and hasClickAction()).performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { BootstrapStore(context).states.first().locked })
    }
}
