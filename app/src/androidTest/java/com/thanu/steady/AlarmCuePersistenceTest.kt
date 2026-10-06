package com.thanu.steady

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.domain.*
import com.thanu.steady.platform.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AlarmCuePersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var now = ActivityClock(10_000,5_000,3)
    private fun store() = AlarmCueStore(context) { now }
    @Before fun setup() = runBlocking { store().clear() }
    @After fun cleanup() = runBlocking { store().clear() }
    @Test fun lockedDeliveryUsesOnlyOpaqueMetadataAndAtomicClaimSurvivesReopen() = runBlocking {
        val cues = store()
        val token = AlarmCueToken("ACTIVITY",UUID.randomUUID().toString(),2,3,20_000,0,deadlineElapsed=5_000)
        cues.register(token)
        val notifications = java.util.concurrent.atomic.AtomicInteger()
        val delivery = BackgroundCueDelivery(cues,{ false },{ _,_,_ -> error("Private provider must remain closed") },{ notifications.incrementAndGet() })
        val results = (1..20).map { async(Dispatchers.IO) { delivery.timer(token.kind,token.id,token.generation) } }.awaitAll()
        assertEquals(1,results.count { it }); assertEquals(1,notifications.get())
        store().register(token.copy(expiresWall = 21_000))
        assertNull(store().claim(token.kind,token.id,token.generation))
        cues.cancel(token.kind,token.id)
        assertNull(cues.claim(token.kind,token.id,token.generation))
        cues.register(token.copy(generation=3))
        assertNull(cues.claim(token.kind,token.id,2))
        now = now.copy(boot=4)
        assertNull(cues.claim(token.kind,token.id,3))
        now = now.copy(boot=3,wall=20_000)
        assertNull(cues.claim(token.kind,token.id,3))
    }
    @Test fun receiptAcknowledgementCannotEraseANewDeliveryAndDatesRemainDistinct() = runBlocking {
        val cues = store()
        val token = AlarmCueToken("BLOCK",UUID.randomUUID().toString(),0,3,20_000,wallAt=10_000,day="2026-01-05")
        val next = token.copy(day="2026-01-06")
        cues.register(token); cues.register(next)
        assertNotEquals(token.key,next.key)
        assertNotNull(cues.claim(token.kind,token.id,0,token.day,token.wallAt))
        val receipts = cues.tokens().filter { it.delivered }
        assertNotNull(cues.claim(next.kind,next.id,0,next.day,next.wallAt))
        cues.acknowledgeRoutines(receipts)
        assertEquals(listOf(next.key),cues.tokens().map { it.key })
        cues.invalidateScheduled()
        assertEquals(listOf(next.key),store().tokens().map { it.key })
    }
}
