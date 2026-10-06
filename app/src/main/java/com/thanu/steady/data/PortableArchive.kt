package com.thanu.steady.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Versioned organiser allowlist. Private Safety and credentials have no fields in this contract. */
@Serializable
data class ExpandedArchive(
    val tasks: List<PlanItem> = emptyList(),
    val habits: List<HabitDefinition> = emptyList(),
    val versions: List<HabitVersion> = emptyList(),
    val occurrences: List<HabitOccurrence> = emptyList(),
    val logs: List<HabitLog> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val sessions: List<ActivitySession> = emptyList(),
    val segments: List<ActivitySegment> = emptyList(),
    val notes: List<SessionNote> = emptyList(),
    val sets: List<ExerciseSet> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val water: List<WaterLog> = emptyList(),
    val sleep: List<SleepLog> = emptyList(),
    val foods: List<FoodIdeaRecord> = emptyList(),
    val meals: List<MealLog> = emptyList(),
    val care: List<CareReminder> = emptyList(),
    val careLogs: List<CareLog> = emptyList(),
    val reflections: List<Reflection> = emptyList(),
    val captures: List<Capture> = emptyList(),
    val interruptions: List<InterruptionEvent> = emptyList(),
    val observations: List<ActivityObservation> = emptyList(),
    val routes: List<RoutePoint> = emptyList(),
    val estimates: List<EnergyEstimate> = emptyList(),
    val days: List<DaySettings> = emptyList(),
    val profile: ExpandedProfile? = null,
    val preferences: AppPreferences? = null
) {
    val recordCount get() = tasks.size + habits.size + versions.size + occurrences.size + logs.size + subjects.size + topics.size + sessions.size + segments.size + notes.size + sets.size + templates.size + water.size + sleep.size + foods.size + meals.size + care.size + careLogs.size + reflections.size + captures.size + interruptions.size + observations.size + routes.size + estimates.size + days.size + (if (profile == null) 0 else 1) + (if (preferences == null) 0 else 1)
}

@Serializable private data class PortableEnvelope(val schema: Int, val legacy: String, val organiser: ExpandedArchive)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object PortableCodec {
    const val MAX_BYTES = 25 * 1024 * 1024
    const val MAX_RECORDS = 100_000
    private val format = Json { encodeDefaults = true; ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false }
    fun encode(value: RecoverySnapshot): String {
        val archive = requireNotNull(value.expanded)
        validate(archive)
        val text = format.encodeToString(PortableEnvelope(2, RecoveryCodec.encode(value.copy(expanded = null)), archive))
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        return text
    }
    fun decode(text: String): RecoverySnapshot {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        StrictJsonStructure.check(text)
        val root = format.parseToJsonElement(text).jsonObject
        val version = root.getValue("schema").jsonPrimitive.int
        if (version == 1) return RecoveryCodec.decode(text)
        require(version == 2)
        validateTypes(root, PortableEnvelope.serializer().descriptor)
        val envelope = format.decodeFromString<PortableEnvelope>(text)
        validate(envelope.organiser)
        StrictJsonStructure.check(envelope.legacy)
        val legacy = RecoveryCodec.decode(envelope.legacy)
        require(envelope.organiser.recordCount + legacy.plans.size + legacy.reviews.size + legacy.timers.size <= MAX_RECORDS)
        return legacy.copy(expanded = envelope.organiser)
    }
    private fun validateTypes(value: JsonElement, descriptor: SerialDescriptor, allowDefaults: Boolean = false) {
        if (value == JsonNull) { require(descriptor.isNullable); return }
        when (descriptor.kind) {
            PrimitiveKind.STRING -> require(value is JsonPrimitive && value.isString)
            PrimitiveKind.BOOLEAN -> require(value is JsonPrimitive && !value.isString && value.booleanOrNull != null)
            PrimitiveKind.INT, PrimitiveKind.LONG -> require(value is JsonPrimitive && !value.isString && value.longOrNull != null)
            PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> require(value is JsonPrimitive && !value.isString && value.doubleOrNull?.isFinite() == true)
            StructureKind.LIST -> { require(value is JsonArray); value.forEach { validateTypes(it, descriptor.getElementDescriptor(0), allowDefaults) } }
            StructureKind.CLASS -> {
                require(value is JsonObject)
                val names = (0 until descriptor.elementsCount).map(descriptor::getElementName)
                require(value.keys.all { it in names })
                names.forEachIndexed { i, key ->
                    require(key in value || allowDefaults && descriptor.isElementOptional(i))
                    value[key]?.let { validateTypes(it, descriptor.getElementDescriptor(i), allowDefaults) }
                }
            }
            else -> error("Unsupported archive structure")
        }
    }
    fun validate(a: ExpandedArchive) {
        require(a.recordCount <= MAX_RECORDS)
        fun id(value: String) { require(UUID.fromString(value).toString() == value) }
        fun text(value: String, limit: Int = 100_000) { require(value.length <= limit) }
        fun day(value: String) { require(value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))); LocalDate.parse(value) }
        fun policy(zone: String, boundary: Int) { ZoneId.of(zone); require(boundary in 0..1439) }
        fun ids(values: List<String>): Set<String> { require(values.distinct().size == values.size); values.forEach(::id); return values.toSet() }
        val taskIds = ids(a.tasks.map { it.id }); val habitIds = ids(a.habits.map { it.id })
        val versionIds = ids(a.versions.map { it.id }); val occurrenceIds = ids(a.occurrences.map { it.id })
        val versionById = a.versions.associateBy { it.id }
        val sessionById = a.sessions.associateBy { it.id }
        ids(a.logs.map { it.id }); val subjectIds = ids(a.subjects.map { it.id }); ids(a.topics.map { it.id })
        val sessionIds = ids(a.sessions.map { it.id }); ids(a.segments.map { it.id })
        ids(a.sets.map { it.id }); ids(a.templates.map { it.id }); ids(a.water.map { it.id }); ids(a.sleep.map { it.id })
        val foodIds = ids(a.foods.map { it.id }); ids(a.meals.map { it.id }); val careIds = ids(a.care.map { it.id })
        ids(a.careLogs.map { it.id }); ids(a.reflections.map { it.id }); ids(a.captures.map { it.id })
        ids(a.interruptions.map { it.id }); ids(a.observations.map { it.id }); ids(a.routes.map { it.id }); ids(a.estimates.map { it.id })
        require(a.days.map { it.day }.distinct().size == a.days.size)
        require(a.notes.map { it.id }.distinct().size == a.notes.size)
        a.notes.forEach { require(!it.id.startsWith("draft:") && !it.id.startsWith("rest:") && !it.id.startsWith("delivery:") && it.id.length <= 200); text(it.text); require(it.sessionId == null || it.sessionId in sessionIds)
            if(it.id.startsWith("ambient:") || it.id.startsWith("study-block")) {
                require(it.sessionId == null)
                StrictJsonStructure.check(it.text)
                val json = format.parseToJsonElement(it.text)
                when {
                    it.id == "ambient:preferences" -> {
                        validateTypes(json, com.thanu.steady.platform.AmbientPreferences.serializer().descriptor, true)
                        format.decodeFromString<com.thanu.steady.platform.AmbientPreferences>(it.text).validate()
                    }
                    it.id == "study-block-settings" -> {
                        validateTypes(json, com.thanu.steady.domain.StudyBlockSettings.serializer().descriptor, true)
                        format.decodeFromString<com.thanu.steady.domain.StudyBlockSettings>(it.text)
                    }
                    it.id.startsWith("study-block:") -> {
                        validateTypes(json, com.thanu.steady.domain.StudyBlockProposal.serializer().descriptor, true)
                        val block = format.decodeFromString<com.thanu.steady.domain.StudyBlockProposal>(it.text).also { b -> b.validate() }
                        require(it.id == "study-block:${block.id}")
                    }
                    else -> error("Unknown organiser metadata")
                }
            }
            if(it.id.startsWith("program:")) {
                require(it.sessionId != null && it.id == "program:${it.sessionId}" && sessionById[it.sessionId]?.kind == "INTERVALS")
                Json.decodeFromString<com.thanu.steady.domain.IntervalProgram>(it.text).validate()
            }
        }
        a.tasks.forEach { day(it.day); policy(it.zone,it.boundary); text(it.title,500); require(it.title.isNotBlank()); text(it.notes)
            require(it.state in setOf("PENDING","COMPLETED","ARCHIVED") && it.priority in 0..2)
            require(it.category in setOf("GENERAL","STUDY","BUILD","MOVEMENT","CARE"))
            require(it.subjectId == null || it.subjectId in subjectIds)
            require(it.plannedSeconds == null || it.plannedSeconds in 1..86_400)
            require(it.timeMinutes == null || it.timeMinutes in 0..1439) }
        a.habits.forEach { it.archivedDay?.let(::day) }
        require(a.versions.map { it.habitId to it.effectiveDay }.distinct().size == a.versions.size)
        a.versions.forEach { require(it.habitId in habitIds); day(it.effectiveDay); day(it.anchorDay); text(it.title,500); text(it.unit,30)
            require(it.title.isNotBlank() && it.type in setOf("CHECKBOX","COUNT","DURATION","QUANTITY"))
            require(it.target.isFinite() && it.target > 0 && it.weekdays in 1..127 && it.everyDays in 1..3650)
            require(it.reminderMinute == null || it.reminderMinute in 0..1439) }
        require(a.occurrences.map { it.habitId to it.day }.distinct().size == a.occurrences.size)
        a.occurrences.forEach { require(it.habitId in habitIds && it.versionId in versionIds); day(it.day); text(it.notes)
            require(versionById.getValue(it.versionId).habitId == it.habitId)
            require(it.quantity.isFinite() && it.quantity >= 0 && it.state in setOf("PENDING","PARTIAL","COMPLETED","SKIPPED","MISSING")) }
        a.logs.forEach { require(it.occurrenceId in occurrenceIds && it.quantity.isFinite() && it.source == "USER"); require(it.undoneAt == null || it.undoneAt >= it.at) }
        a.subjects.forEach { text(it.title,500); text(it.code,100); require(it.title.isNotBlank()) }
        a.topics.forEach { require(it.subjectId in subjectIds && it.state in setOf("NEW","ACTIVE","DONE")); text(it.title,500); require(it.title.isNotBlank()) }
        require(a.sessions.count { it.state in setOf("RUNNING","PAUSED","INTERRUPTED") } <= 1)
        a.sessions.forEach { policy(it.zone,it.boundary); text(it.title,500); text(it.notes)
            require(it.type in setOf("FOCUS","WORKOUT","BREAK","REST","INTERVAL"))
            require(it.state in setOf("IDLE","RUNNING","PAUSED","INTERRUPTED","COMPLETED","STOPPED","DISCARDED"))
            require(it.subjectId == null || it.subjectId in subjectIds); require(it.taskId == null || it.taskId in taskIds)
            require(it.activeMillis in 0..86_400_000 && it.breakSeconds in 0..86_400)
            require(it.plannedSeconds == null || it.plannedSeconds in 1..86_400)
            require(it.generation in 0 until Int.MAX_VALUE && it.cueFlags in 0..7 && it.boot >= 0)
            require(it.effort == null || it.effort in 1..10)
            require(it.started == null || it.ended == null || it.ended >= it.started)
            require(it.elapsedAnchor == null || it.elapsedAnchor >= 0)
            require(it.deadlineElapsed == null || it.deadlineElapsed >= 0) }
        a.segments.forEach { require(it.sessionId in sessionIds); policy(it.zone,it.boundary)
            require(it.activeMillis in 0..86_400_000 && it.elapsedStart >= 0)
            require(it.endWall == null || it.endWall >= it.startWall)
            require(it.elapsedEnd == null || it.elapsedEnd >= it.elapsedStart)
            require(it.endWall == null || it.endWall - it.startWall == it.activeMillis) }
        a.sets.forEach { require(it.sessionId in sessionIds && sessionById.getValue(it.sessionId).type == "WORKOUT")
            text(it.exercise,500); text(it.notes,10_000); require(it.exercise.isNotBlank() && it.reps in 0..10_000 && it.unit in setOf("kg","lb"))
            require(it.load == null || it.load.isFinite() && it.load >= 0) }
        a.templates.forEach { text(it.title,500); text(it.exercises); require(it.title.isNotBlank())
            require(it.mode in setOf("WALKING","RUNNING","CYCLING","STRENGTH","INTERVALS","MOBILITY","CUSTOM"))
            com.thanu.steady.domain.IntervalProgram(it.workSeconds,it.restSeconds,it.rounds,it.warmupSeconds,it.cooldownSeconds).validate() }
        a.water.forEach { day(it.day); policy(it.zone,it.boundary); require(it.millilitres > 0 && it.source == "USER") }
        a.sleep.forEach { day(it.day); policy(it.zone,it.boundary); text(it.notes)
            require(it.wake > it.bedtime && it.wake - it.bedtime <= 7 * 86_400_000L && (it.restedness == null || it.restedness in 1..5)) }
        a.foods.forEach { text(it.title,500); text(it.ingredients); text(it.instructions); text(it.budget,500); text(it.tags,2000)
            require(it.title.isNotBlank() && (it.prepMinutes == null || it.prepMinutes in 0..1440)); it.reviewedDay?.let(::day) }
        a.meals.forEach { day(it.day); policy(it.zone,it.boundary); text(it.title,500); text(it.notes)
            require(it.title.isNotBlank() && (it.foodId == null || it.foodId in foodIds)) }
        a.care.forEach { text(it.instruction,10_000); require(it.instruction.isNotBlank() && it.weekdays in 1..127 && it.minute in 0..1439) }
        a.careLogs.forEach { require(it.reminderId in careIds); day(it.day); text(it.instructionSnapshot,10_000) }
        require(a.reflections.map { it.startDay to it.endDay }.distinct().size == a.reflections.size)
        a.reflections.forEach { day(it.startDay); day(it.endDay); require(it.startDay <= it.endDay)
            listOf(it.helped,it.demanding,it.evidence,it.adjustment,it.highlight,it.obstacle,it.tomorrow).forEach { s -> text(s) }
            require(it.mood == null || it.mood in 1..5); require(it.energy == null || it.energy in 1..5) }
        a.captures.forEach { text(it.text); require(it.text.isNotBlank()) }
        a.interruptions.forEach { require(it.mode == "VOLUNTARY" && it.appPackage == null && it.pauseSeconds in 0..60 && it.outcome in setOf("RETURNED","CONTINUED","DISABLED")) }
        require(a.observations.map { it.sourceKey }.distinct().size == a.observations.size)
        a.observations.forEach { day(it.day); policy(it.zone,it.boundary); text(it.sourceKey,200); text(it.source,100); text(it.quality,100)
            require(it.end >= it.start && (it.steps == null || it.steps >= 0) && (it.distanceMetres == null || it.distanceMetres.isFinite() && it.distanceMetres >= 0)) }
        require(a.routes.map { it.sessionId to it.at }.distinct().size == a.routes.size)
        a.routes.forEach { require(it.sessionId in sessionIds && it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0)
            require(it.accuracyMetres.isFinite() && it.accuracyMetres >= 0 && it.segment >= 0) }
        require(a.estimates.map { it.sessionId }.distinct().size == a.estimates.size)
        a.estimates.forEach { require(it.sessionId in sessionIds && it.kilograms.isFinite() && it.kilograms in 1.0..1000.0 && it.met.isFinite() && it.met > 0)
            require(it.activeKcal.isFinite() && it.activeKcal >= 0 && it.grossKcal.isFinite() && it.grossKcal >= it.activeKcal); text(it.method,200); text(it.reference,2000) }
        a.days.forEach { day(it.day); policy(it.zone,it.boundary); text(it.nextAction,10_000); require(it.mode in setOf("NORMAL","MINIMUM","PAUSED")) }
        a.profile?.let { p -> com.thanu.steady.domain.PersonalizationRules.validate(p.ringMetrics,p.waterQuickMl,p.waterUnit,p.reviewCards)
            require(p.id == 1 && p.country in setOf("IN","OTHER") && p.palette in setOf("KINETIC","DAYBOOK") && p.theme in setOf("SYSTEM","LIGHT","DARK"))
            text(p.displayName,100); require(p.textScale in 1f..2f && p.alertBudget in 0..5 && p.quietStart in 0..1439 && p.quietEnd in 0..1439)
            require(p.focusTargetMinutes == null || p.focusTargetMinutes in 1..1440)
            require(p.waterTargetMl == null || p.waterTargetMl > 0); require(p.stepTarget == null || p.stepTarget > 0)
            require(p.weightKg == null || p.weightKg.isFinite() && p.weightKg in 1.0..1000.0)
            fun csv(s: String, allowed: Set<String>) { val values = s.split(',').filter(String::isNotBlank); require(values.size == values.distinct().size && values.all { it in allowed }) }
            csv(p.modules,setOf("PLAN","HABITS","FOCUS","MOVEMENT","FOOD","WATER","SLEEP"))
            val cards = setOf("NEXT","RINGS","TIMELINE","HABITS","CAPTURE","FOOD","ROUTINES"); csv(p.dashboard,cards); csv(p.wideCards,cards)
            require(p.zoneMode in setOf("FIXED","DEVICE") && p.dateStyle in setOf("LOCAL","ISO")) }
        a.preferences?.let { require(it.id == 1 && !it.appLock); policy(it.zoneId,it.boundaryMinutes); text(it.avoidFoods,2000); require(it.cueFlags in 0..7) }
    }
}

/** Structural preflight rejects duplicate keys, excessive nesting and oversized strings before decoding. */
internal object StrictJsonStructure {
    fun check(input: String) {
        var index = 0
        fun space() { while (index < input.length && input[index].isWhitespace()) index++ }
        fun string(): String {
            val begin = index; require(input[index++] == '"')
            var escaped = false
            while (index < input.length) {
                val ch = input[index++]
                if (!escaped && ch == '"') return Json.decodeFromString<String>(input.substring(begin,index))
                if (!escaped && ch == '\\') escaped = true else escaped = false
            }
            error("Invalid archive")
        }
        fun value(depth: Int) {
            require(depth <= 8); space(); require(index < input.length)
            when (input[index]) {
                '{' -> { index++; space(); val keys = mutableSetOf<String>()
                    if (input.getOrNull(index) == '}') { index++; return }
                    while (true) { space(); require(input.getOrNull(index) == '"'); require(keys.add(string())); space(); require(input.getOrNull(index++) == ':')
                        value(depth+1); space(); when(input.getOrNull(index++)) { '}' -> return; ',' -> Unit; else -> error("Invalid archive") } } }
                '[' -> { index++; space(); if (input.getOrNull(index) == ']') { index++; return }
                    var count = 0; while (true) { require(++count <= PortableCodec.MAX_RECORDS); value(depth+1); space()
                        when(input.getOrNull(index++)) { ']' -> return; ',' -> Unit; else -> error("Invalid archive") } } }
                '"' -> { val decoded = string(); require(decoded.length <= PortableCodec.MAX_BYTES) }
                else -> { val begin = index; while(index < input.length && input[index] !in ",]} \r\n\t") index++
                    require(index > begin); Json.parseToJsonElement(input.substring(begin,index)) }
            }
        }
        value(0); space(); require(index == input.length)
    }
}
