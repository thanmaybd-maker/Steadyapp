package com.thanu.steady.domain

import com.thanu.steady.data.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.Assert.*

class LegacyOgCodecTest {
    private val fixture = """{"plans":[{"dateStr":"2026-01-05","title":"Synthetic plan","description":"Draft notes","category":"Study","timeSlot":"Morning","estimatedMinutes":30,"isCompleted":false,"priority":"High"}],"sessions":[{"dateStr":"2026-01-04","subject":"Synthetic subject","durationMinutes":25,"mode":"Pomodoro (25m)","rating":4,"notes":"Synthetic notes","timestamp":1767614400000}],"foodIdeas":[{"title":"Synthetic food","prepTimeMinutes":10,"benefits":"Unverified source claim","ingredients":"Synthetic pantry","instructions":"Synthetic preparation","isFavorite":true,"category":"Quick Snack"}],"reviews":[{"dateStr":"2026-01-05","moodRating":4,"energyRating":3,"highlight":"Synthetic highlight","distractionOrObstacle":"Synthetic obstacle","tomorrowPriority":"Synthetic priority","aiInsight":"Unverified generated draft"}],"safetyNotes":[{"content":"Synthetic private exclusion canary"}]}"""
    @Test fun eligibleCollectionsKeepCalendarProvenanceWithoutInventingMeasuredIntervals() {
        val p = LegacyOgCodec.decode(fixture,"Asia/Kolkata",1767614400000)
        assertEquals(listOf(1,1,1,1),p.counts)
        assertFalse(p.preview.contains("Synthetic private exclusion canary"))
        assertFalse(p.archive.notes.any { it.text.contains("Synthetic private exclusion canary") })
        assertEquals(0,p.archive.tasks.single().boundary); assertNull(p.archive.tasks.single().timeMinutes)
        assertEquals(2,p.archive.tasks.single().priority)
        assertEquals(1_500_000L,p.archive.sessions.single().activeMillis)
        assertEquals(LegacyOgCodec.SOURCE,p.archive.sessions.single().source)
        assertTrue(p.archive.segments.isEmpty() && p.archive.habits.isEmpty() && p.archive.templates.isEmpty())
        assertNull(p.archive.sessions.single().effort)
        assertEquals(3,p.archive.reflections.single().energy)
        assertFalse(p.archive.foods.single().vegetarian)
    }
    @Test fun canonicalIdsIgnoreWhitespaceKeyOrderAndPrivateSubtreeButRetainIdenticalDuplicates() {
        val original=Json.parseToJsonElement(fixture).jsonObject
        val reversed=JsonObject(original.toList().reversed().toMap() + ("safetyNotes" to JsonArray(emptyList())))
        val a=LegacyOgCodec.decode(fixture,"Asia/Kolkata",1767614400000)
        val b=LegacyOgCodec.decode(reversed.toString(),"Asia/Kolkata",1767614400010)
        assertEquals(a.archive.tasks.map { it.id },b.archive.tasks.map { it.id })
        assertEquals(a.archive.notes.map { it.id }.toSet(),b.archive.notes.map { it.id }.toSet())
        val twice=JsonObject(original + ("plans" to JsonArray(List(2) { original.getValue("plans").jsonArray.single() })))
        assertEquals(2,LegacyOgCodec.decode(twice.toString(),"UTC",1767614400000).archive.tasks.map { it.id }.distinct().size)
    }
    @Test fun malformedCoercedDuplicateUnknownAndOversizedFilesAreRefusedBeforeWrites() {
        val invalid=listOf(fixture.replace("\"estimatedMinutes\":30","\"estimatedMinutes\":\"30\""),
            fixture.replace("\"rating\":4","\"rating\":6"),fixture.replace("\"isCompleted\":false","\"isCompleted\":\"false\""),
            fixture.replace("\"dateStr\":\"2026-01-05\"","\"dateStr\":\"2026-02-30\""),
            fixture.replace("\"title\":\"Synthetic plan\"","\"title\":\"Synthetic plan\",\"title\":\"Duplicate\""),
            fixture.replace("\"priority\":\"High\"","\"priority\":\"High\",\"unknown\":1"),
            " ".repeat(LegacyOgCodec.MAX_BYTES+1))
        invalid.forEach { assertTrue(runCatching { LegacyOgCodec.decode(it,"UTC",1767614400000) }.isFailure) }
    }
}
