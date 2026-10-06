package com.thanu.steady.data

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Receipts preserve eligible source fields and remember imports even after a record is deleted. */
@Serializable data class LegacyOgReceipt(val id: String, val kind: String, val original: String) {
    fun validate() {
        require(UUID.fromString(id).toString() == id)
        require(original.length <= LegacyOgCodec.MAX_BYTES)
        StrictJsonStructure.check(original)
        LegacyOgCodec.checkedRow(kind, Json.parseToJsonElement(original).jsonObject)
    }
}

data class LegacyOgImport(val archive: ExpandedArchive, val counts: List<Int>, val preview: String,
    val zone: String, val firstDay: String?, val lastDay: String?)
data class LegacyOgImportResult(val added: Int, val skipped: Int)

object LegacyOgCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    const val MAX_ROWS = 5000
    const val SOURCE = "LEGACY_OG_CALENDAR_REPORTED"
    private val schemas = mapOf(
        "plans" to setOf("dateStr","title","description","category","timeSlot","estimatedMinutes","isCompleted","priority"),
        "sessions" to setOf("dateStr","subject","durationMinutes","mode","rating","notes","timestamp"),
        "foodIdeas" to setOf("title","prepTimeMinutes","benefits","ingredients","instructions","isFavorite","category"),
        "reviews" to setOf("dateStr","moodRating","energyRating","highlight","distractionOrObstacle","tomorrowPriority","aiInsight"))
    private fun JsonObject.text(key: String, max: Int = 100_000): String {
        val p = getValue(key) as? JsonPrimitive ?: error("Invalid legacy field")
        require(p.isString && p.content.length <= max); return p.content
    }
    private fun JsonObject.number(key: String, range: LongRange): Long {
        val p = getValue(key) as? JsonPrimitive ?: error("Invalid legacy field")
        require(!p.isString); return requireNotNull(p.longOrNull).also { require(it in range) }
    }
    private fun JsonObject.flag(key: String): Boolean {
        val p = getValue(key) as? JsonPrimitive ?: error("Invalid legacy field")
        require(!p.isString); return requireNotNull(p.booleanOrNull)
    }
    private fun JsonObject.day(): String = text("dateStr",10).also {
        require(it.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))); require(LocalDate.parse(it).year in 1900..9999)
    }
    internal fun checkedRow(kind: String, o: JsonObject) {
        require(o.keys == requireNotNull(schemas[kind]))
        when(kind) {
            "plans" -> {
                o.day(); require(o.text("title",500).isNotBlank()); o.text("description")
                o.text("category",100); o.text("timeSlot",100); o.number("estimatedMinutes",1L..1440L)
                o.flag("isCompleted"); require(o.text("priority",10) in setOf("Low","Medium","High"))
            }
            "sessions" -> {
                o.day(); require(o.text("subject",500).isNotBlank()); o.number("durationMinutes",1L..1440L)
                o.text("mode",500); o.number("rating",1L..5L); o.text("notes")
                o.number("timestamp",0L..253402300799999L)
            }
            "foodIdeas" -> {
                require(o.text("title",500).isNotBlank()); o.number("prepTimeMinutes",0L..1440L)
                o.text("benefits"); o.text("ingredients"); o.text("instructions"); o.flag("isFavorite"); o.text("category",2000)
            }
            "reviews" -> {
                o.day(); o.number("moodRating",1L..5L); o.number("energyRating",1L..5L)
                o.text("highlight"); o.text("distractionOrObstacle"); o.text("tomorrowPriority"); o.text("aiInsight")
            }
        }
    }
    fun decode(text: String, zone: String, importedAt: Long): LegacyOgImport {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES && importedAt >= 0)
        val zoneId = ZoneId.of(zone)
        StrictJsonStructure.check(text)
        val root = Json.parseToJsonElement(text) as? JsonObject ?: error("Invalid legacy file")
        require(root.keys == schemas.keys || root.keys == schemas.keys + "safetyNotes")
        // Its contents have no mapping, receipt, preview or log representation.
        root["safetyNotes"]?.let { require(it is JsonArray) }
        val arrays = schemas.keys.associateWith { key -> root.getValue(key) as? JsonArray ?: error("Invalid legacy collection") }
        require(arrays.values.sumOf { it.size } in 1..MAX_ROWS)
        val tasks = mutableListOf<PlanItem>(); val sessions = mutableListOf<ActivitySession>()
        val foods = mutableListOf<FoodIdeaRecord>(); val reviews = mutableListOf<Reflection>(); val receipts = mutableListOf<SessionNote>()
        val days = mutableListOf<String>(); val preview = linkedMapOf<String,JsonElement>()
        arrays.forEach { (kind, rows) ->
            val duplicates = mutableMapOf<String,Int>()
            val originals = rows.map { row ->
                val o = row as? JsonObject ?: error("Invalid legacy row"); checkedRow(kind,o)
                val canonical = JsonObject(o.toSortedMap()).toString()
                val ordinal = duplicates.getOrDefault(canonical,0); duplicates[canonical] = ordinal + 1
                val id = UUID.nameUUIDFromBytes("steady-og-v1|$kind|$canonical|$ordinal".toByteArray(Charsets.UTF_8)).toString()
                val receipt = LegacyOgReceipt(id,kind,canonical)
                receipts += SessionNote("legacy-og:$id",null,Json.encodeToString(receipt),importedAt)
                when(kind) {
                    "plans" -> {
                        val day = o.day(); days += day
                        val category = when(o.text("category",100).lowercase(java.util.Locale.ROOT)) {
                            "study" -> "STUDY"; "build" -> "BUILD"; "food" -> "FOOD"; "wellness" -> "MOVEMENT"; else -> "GENERAL"
                        }
                        tasks += PlanItem(id,day,o.text("title"),o.text("description"),category,
                            plannedSeconds=o.number("estimatedMinutes",1L..1440L)*60,
                            priority=when(o.text("priority")) { "High" -> 2; "Low" -> 0; else -> 1 },
                            state=if(o.flag("isCompleted")) "COMPLETED" else "PENDING",
                            position=tasks.size,created=importedAt,updated=importedAt,zone=zone,boundary=0)
                    }
                    "sessions" -> {
                        val day = o.day(); days += day
                        // Calendar anchor only: OG exports aggregate duration, not measured active intervals.
                        val anchor = LocalDate.parse(day).atTime(12,0).atZone(zoneId).toInstant().toEpochMilli()
                        sessions += ActivitySession(id,"FOCUS","LEGACY_OG",o.text("subject"),state="COMPLETED",
                            activeMillis=o.number("durationMinutes",1L..1440L)*60_000,started=anchor,ended=anchor,
                            notes=o.text("notes"),zone=zone,boundary=0,source=SOURCE,cueFlags=0,updated=importedAt)
                    }
                    "foodIdeas" -> foods += FoodIdeaRecord(id,o.text("title"),o.text("ingredients"),o.text("instructions"),
                        prepMinutes=o.number("prepTimeMinutes",0L..1440L).toInt(),tags=o.text("category"),vegetarian=false,
                        favorite=o.flag("isFavorite"),provenance="LEGACY_OG_UNVERIFIED",updated=importedAt)
                    "reviews" -> {
                        val day = o.day(); days += day
                        reviews += Reflection(id,day,day,evidence=o.text("aiInsight"),highlight=o.text("highlight"),
                            obstacle=o.text("distractionOrObstacle"),tomorrow=o.text("tomorrowPriority"),
                            mood=o.number("moodRating",1L..5L).toInt(),energy=o.number("energyRating",1L..5L).toInt(),updated=importedAt)
                    }
                }
                JsonObject(o.toSortedMap())
            }
            preview[kind] = JsonArray(originals)
        }
        require(reviews.map { it.startDay }.distinct().size == reviews.size)
        val archive = ExpandedArchive(tasks=tasks,sessions=sessions,foods=foods,reflections=reviews,notes=receipts)
        PortableCodec.validate(archive)
        return LegacyOgImport(archive,listOf(tasks.size,sessions.size,foods.size,reviews.size),
            Json { prettyPrint=true }.encodeToString(JsonObject(preview)),zone,days.minOrNull(),days.maxOrNull())
    }
}

/** Append-only, atomic organiser merge. Never opens private storage or activates imported alarms. */
class LegacyOgImporter(private val provider: () -> SteadyDatabase) {
    suspend fun apply(import: LegacyOgImport): LegacyOgImportResult {
        val a = import.archive; PortableCodec.validate(a)
        val db = provider()
        return db.withTransaction {
            val dao = db.expandedDao(); var added = 0; var skipped = 0
            require(db.portableDao().count() + a.recordCount <= PortableCodec.MAX_RECORDS)
            val receipts = a.notes.associateBy { it.id }
            suspend fun adopt(id: String, exists: suspend () -> Boolean, save: suspend () -> Unit) {
                val receipt = requireNotNull(receipts["legacy-og:$id"])
                if(dao.note(receipt.id) != null) { skipped++; return }
                if(exists()) skipped++ else { save(); added++ }
                dao.save(receipt)
            }
            a.tasks.forEach { p -> adopt(p.id,{ dao.task(p.id) != null },{ dao.save(p) }) }
            a.sessions.forEach { s -> adopt(s.id,{ dao.session(s.id) != null },{ dao.save(s) }) }
            a.foods.forEach { f -> adopt(f.id,{ dao.food(f.id) != null },{ dao.save(f) }) }
            a.reflections.forEach { r -> adopt(r.id,{ dao.reflection(r.startDay,r.endDay) != null },{ dao.save(r) }) }
            LegacyOgImportResult(added,skipped)
        }
    }
}
