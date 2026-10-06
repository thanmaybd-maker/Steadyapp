package com.thanu.steady

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityClock
import com.thanu.steady.platform.*
import com.thanu.steady.ui.ExpandedViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.UUID

class ReviewDraftPersistenceTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val name="review-draft-synthetic-${UUID.randomUUID()}.db"
    private lateinit var db: SteadyDatabase
    private val clock=Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"),ZoneOffset.UTC)
    private val day="2026-01-05"
    @Before fun setup() {
        check(context.packageName == "com.thanu.steady.qa"); System.loadLibrary("sqlcipher")
        db=Room.databaseBuilder(context,SteadyDatabase::class.java,name)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes))).build()
    }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }
    private fun repo()=ExpandedRepository({ db },clock,PreferencesRepository { db })
    @Test fun optionalRatingsAndProseSurviveInvalidWritesHistoryChangesAndPortableRestore() = runBlocking {
        val repo=repo()
        val original=Reflection(UUID.randomUUID().toString(),day,day,helped="Synthetic prose",mood=3,energy=4,updated=clock.millis())
        repo.saveReflection(original)
        assertTrue(runCatching { repo.saveReflection(original.copy(mood=6)) }.isFailure)
        assertTrue(db.expandedDao().reflection(day,day) == original)
        val log=WaterLog(UUID.randomUUID().toString(),day,250,clock.millis(),"UTC",0)
        repo.saveWater(log); repo.deleteWater(log.id)
        val payload=PortableCodec.encode(RecoveryRepository { db }.snapshot())
        RecoveryRepository { db }.replace(PortableCodec.decode(payload))
        assertTrue(repo.snapshot(LocalDate.parse(day),LocalDate.parse(day)).reflection == original)
        repo.saveReflection(original.copy(mood=null,energy=null))
        assertTrue(db.expandedDao().reflection(day,day)?.let { it.mood == null && it.energy == null && it.helped == original.helped } == true)
    }
    @Test fun oldEncryptedDraftRetainsNewFieldDefaultsAndRefusedDraftLoadRequiresRetry() = runBlocking {
        val repo=repo(); val preferences=PreferencesRepository { db }
        repo.saveProfile(ExpandedProfile(onboarded=true))
        repo.saveNote(SessionNote("draft:synthetic-old",null,"{\"helped\":\"Synthetic retained prose\"}",clock.millis()))
        repo.saveNote(SessionNote("draft:synthetic-refused",null,"not-json",clock.millis()))
        val store=ViewModelStore(); lateinit var model: ExpandedViewModel
        instrumentation.runOnMainSync {
            model=ExpandedViewModel(repo,ActivityRepository({ db },{ ActivityClock(clock.millis(),1000,1) },preferences),preferences,
                ActivityAlarmAdapter(context),NotificationAdapter(context),BootstrapStore(context),PlatformSensors(context),AudioSoundscapeEngine(context),{ false })
            store.put("synthetic",model)
        }
        try {
            withTimeout(5000) { model.state.first { it.period != null } }
            instrumentation.runOnMainSync { model.openDraft("synthetic-old",mapOf("helped" to "","mood" to "3","energy" to "4")) }
            withTimeout(5000) { model.state.first { "synthetic-old" !in it.draftLoading } }
            assertTrue(model.drafts.value["synthetic-old"] == mapOf("helped" to "Synthetic retained prose","mood" to "3","energy" to "4"))
            instrumentation.runOnMainSync { model.openDraft("synthetic-refused",mapOf("helped" to "Synthetic initial")) }
            withTimeout(5000) { model.state.first { "synthetic-refused" in it.draftLoadFailed && "synthetic-refused" !in it.draftLoading } }
            instrumentation.runOnMainSync { model.field("synthetic-refused","helped","Must not replace unread draft") }
            assertTrue(model.drafts.value["synthetic-refused"]?.get("helped") == "Synthetic initial")
            repo.saveNote(SessionNote("draft:synthetic-refused",null,"{\"helped\":\"Synthetic retried draft\"}",clock.millis()))
            instrumentation.runOnMainSync { model.retryDraft("synthetic-refused") }
            withTimeout(5000) { model.state.first { "synthetic-refused" !in it.draftLoading && "synthetic-refused" !in it.draftLoadFailed } }
            assertTrue(model.drafts.value["synthetic-refused"]?.get("helped") == "Synthetic retried draft")
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }
}
