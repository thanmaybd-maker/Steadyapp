package com.thanu.steady

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import kotlinx.coroutines.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.UUID

class FoodPlanningPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "food-synthetic-${UUID.randomUUID()}.db"
    private val key = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
    private var database: SteadyDatabase? = null
    private val day = LocalDate.parse("2026-01-05")
    private val clock = Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"),ZoneOffset.UTC)
    @Before fun setup() { System.loadLibrary("sqlcipher") }
    private fun open() = Room.databaseBuilder(context,SteadyDatabase::class.java,name)
        .openHelperFactory(SupportOpenHelperFactory(key.copyOf())).build().also { database=it }
    @After fun cleanup() { database?.close(); context.deleteDatabase(name); key.fill(0) }
    private fun repository() = ExpandedRepository({ requireNotNull(database) },clock,PreferencesRepository { requireNotNull(database) })
    @Test fun recipeContextAndRepeatSafePlanRoundTripWithoutInventingConsumption() = runBlocking {
        var db = open()
        val recipe = FoodIdeaRecord(UUID.randomUUID().toString(),"Synthetic recipe","Synthetic ingredients","Synthetic instructions",prepMinutes=10,updated=clock.millis())
        val info = RecipeContext("LUNCH","Synthetic chosen context")
        repository().saveRecipe(recipe,info)
        assertTrue(runCatching { repository().saveRecipe(recipe.copy(title="Invalid update"),RecipeContext("INVALID")) }.isFailure)
        assertEquals(recipe.title,db.expandedDao().food(recipe.id)!!.title)
        val proposal = MealPlanProposal(UUID.randomUUID().toString(),recipe.id,day.toString(),recipe.title,recipe.instructions,780,15,"UTC",240)
        val ids = (1..12).map { async(Dispatchers.IO) { repository().adoptMeal(proposal) } }.awaitAll()
        assertEquals(1,ids.distinct().size)
        val plan = db.expandedDao().task(proposal.planId)!!
        assertEquals("FOOD",plan.category); assertEquals(900L,plan.plannedSeconds); assertEquals("PENDING",plan.state)
        assertTrue(db.expandedDao().meals(day.toString(),day.toString()).isEmpty())
        assertTrue(db.expandedDao().sessions(clock.millis()-86_400_000,clock.millis()+86_400_000).isEmpty())
        val snapshot = RecoveryRepository { db }.snapshot()
        val payload = PortableCodec.encode(snapshot)
        assertFalse(repository().snapshot(day,day).notes.any { it.id.startsWith("recipe-context:") || it.id.startsWith("meal-adoption:") })
        db.close(); db=open()
        assertEquals(info,repository().recipeContext(recipe.id))
        repository().saveTask(plan.copy(title="Synthetic renamed",day=day.plusDays(1).toString()))
        assertEquals(plan.id,repository().adoptMeal(proposal))
        assertEquals("Synthetic renamed",db.expandedDao().task(plan.id)!!.title)
        repository().deleteTask(plan.id)
        assertEquals(plan.id,repository().adoptMeal(proposal)); assertNull(db.expandedDao().task(plan.id))
        RecoveryRepository { db }.replace(PortableCodec.decode(payload))
        assertEquals(info,repository().recipeContext(recipe.id)); assertNotNull(db.expandedDao().task(plan.id))
        repository().deleteFood(recipe.id)
        assertNull(db.expandedDao().note("recipe-context:${recipe.id}"))
        PortableCodec.encode(RecoveryRepository { db }.snapshot()) // Deleted-source association remains a valid explicit history marker.
        Unit
    }
}
