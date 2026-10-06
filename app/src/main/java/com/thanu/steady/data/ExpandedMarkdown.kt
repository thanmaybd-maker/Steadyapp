package com.thanu.steady.data

import com.thanu.steady.domain.ActivityTotals
import com.thanu.steady.domain.WallInterval
import java.time.ZoneId

/** Explicit, readable organiser scope; the formatter has no access to either Safety DAO. */
object ExpandedMarkdown {
    val categories = setOf("TASKS","HABITS","FOCUS","WORKOUTS","WATER","SLEEP","FOOD","CARE","REFLECTION","CAPTURE")
    fun generate(p: PeriodSnapshot,selected: Set<String>,includeNotes: Boolean): String {
        require(selected.isNotEmpty() && selected.all { it in categories })
        fun line(value: String) = value.replace("\r","").replace("\n","\n    ")
        return buildString {
            appendLine("# Steady records"); appendLine(); appendLine("Period: ${p.start} to ${p.end}")
            appendLine("Scope: ${selected.sorted().joinToString(", ")}"); appendLine()
            fun section(title: String,body: () -> Unit) { appendLine("## $title"); appendLine(); body(); appendLine() }
            if("TASKS" in selected) section("Tasks") {
                p.tasks.forEach { appendLine("- ${it.day} · ${line(it.title)} · ${it.state}")
                    if(includeNotes && it.notes.isNotBlank()) appendLine("    Notes: ${line(it.notes)}") }
            }
            if("HABITS" in selected) section("Habit occurrences") {
                val versions = p.versions.associateBy { it.id }
                p.occurrences.forEach { occurrence -> val version = versions[occurrence.versionId] ?: return@forEach
                    appendLine("- ${occurrence.day} · ${line(version.title)} · ${occurrence.quantity} / ${version.target} ${line(version.unit)} · ${occurrence.state}")
                    if(includeNotes && occurrence.notes.isNotBlank()) appendLine("    Notes: ${line(occurrence.notes)}") }
            }
            val sessions = p.sessions.associateBy { it.id }
            fun activities(type: String) {
                p.sessions.filter { it.type == type && it.state != "DISCARDED" }.forEach { s ->
                    val spans = p.segments.filter { it.sessionId == s.id && it.endWall != null }.mapNotNull {
                        val first = ActivityTotals.dayBounds(p.start,ZoneId.of(it.zone),it.boundary).start
                        val last = ActivityTotals.dayBounds(p.end,ZoneId.of(it.zone),it.boundary).end
                        val begin = maxOf(first,it.startWall); val end = minOf(last,it.endWall!!)
                        if(end > begin) WallInterval(begin,end) else null
                    }
                    val actual = ActivityTotals.unionMillis(spans)/1000
                    appendLine("- ${line(s.title)} · ${s.kind} · ${actual} actual seconds in period · ${s.state} · ${s.source}")
                    if(includeNotes && s.notes.isNotBlank()) appendLine("    Notes: ${line(s.notes)}")
                }
            }
            if("FOCUS" in selected) section("Focus") { activities("FOCUS") }
            if("WORKOUTS" in selected) section("Movement") { activities("WORKOUT") }
            if("WATER" in selected) section("Water") { p.water.forEach { appendLine("- ${it.day} · ${it.millilitres} ml · ${it.source}") } }
            if("SLEEP" in selected) section("Sleep") { p.sleep.forEach {
                appendLine("- ${it.day} · ${(it.wake-it.bedtime)/60000.0} actual minutes · ${it.source}")
                if(includeNotes && it.notes.isNotBlank()) appendLine("    Notes: ${line(it.notes)}")
            } }
            if("FOOD" in selected) section("Meals") { p.meals.forEach { appendLine("- ${it.day} · ${line(it.title)}")
                if(includeNotes && it.notes.isNotBlank()) appendLine("    Notes: ${line(it.notes)}") } }
            if("CARE" in selected) section("Care records") { p.careLogs.forEach { appendLine("- ${it.day} · ${line(it.instructionSnapshot)} · marked done") } }
            if("REFLECTION" in selected) section("Reflection") { p.reflection?.let { r ->
                listOf("Helped" to r.helped,"Demanding" to r.demanding,"Evidence" to r.evidence,"Adjustment" to r.adjustment,
                    "Highlight" to r.highlight,"Obstacle" to r.obstacle,"Tomorrow" to r.tomorrow).filter { it.second.isNotBlank() }
                    .forEach { appendLine("- ${it.first}: ${line(it.second)}") }
            } }
            if("CAPTURE" in selected) section("Captures") { p.captures.filter {
                val day = com.thanu.steady.domain.LogicalDayPolicy().getLogicalDay(java.time.Instant.ofEpochMilli(it.created),ZoneId.of(p.preferences.zoneId),p.preferences.boundaryMinutes)
                day in p.start..p.end
            }.forEach { appendLine("- ${line(it.text)}") } }
            appendLine("Private Safety, contacts, credentials, device keys and route geometry are excluded.")
        }
    }
}
