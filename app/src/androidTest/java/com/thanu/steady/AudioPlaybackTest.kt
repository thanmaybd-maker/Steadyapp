package com.thanu.steady

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.thanu.steady.platform.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*

/** Silent device verification; no owner's records or audio recordings are read. */
class AudioPlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<UiHarnessActivity>()
    private lateinit var engine: AudioSoundscapeEngine
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Before fun prepare() { compose.runOnIdle { engine = AudioSoundscapeEngine(compose.activity); engine.setVolume(0f) } }
    @After fun cleanup() { compose.runOnIdle { engine.release(); scope.cancel() } }

    @Test fun everySoundWritesPcmAndRapidRestartDoesNotReviveStoppedWriter() {
        AmbientSoundType.entries.forEach { sound ->
            compose.runOnIdle { engine.playSoundscape(sound.name); engine.start(scope) }
            compose.waitUntil(5000) { engine.isPlaying.value && engine.framesWritten.value >= 4096 }
            assertEquals(sound.name, engine.currentSoundscape.value)
            assertNull(engine.error.value)
            compose.runOnIdle { engine.stop() }
            assertFalse(engine.isPlaying.value)
        }
        repeat(20) { compose.runOnIdle { engine.start(scope); engine.stop() } }
        compose.runOnIdle { engine.start(scope) }
        compose.waitUntil(5000) { engine.isPlaying.value && engine.framesWritten.value >= 4096 }
        assertNull(engine.error.value)
        compose.runOnIdle { engine.stop() }
        assertFalse(engine.isPlaying.value)
    }
}
