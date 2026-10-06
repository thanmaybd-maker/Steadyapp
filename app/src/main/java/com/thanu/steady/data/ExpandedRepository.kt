package com.thanu.steady.data

import androidx.room.withTransaction
import com.thanu.steady.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.*
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class PeriodSnapshot(val start: LocalDate, val end: LocalDate, val profile: ExpandedProfile,
    val preferences: AppPreferences, val days: List<DaySettings>, val tasks: List<PlanItem>,
    val habits: List<HabitDefinition>, val versions: List<HabitVersion>, val occurrences: List<HabitOccurrence>,
    val sessions: List<ActivitySession>, val segments: List<ActivitySegment>, val active: List<ActivitySession>,
    val subjects: List<Subject>, val water: List<WaterLog>, val sleep: List<SleepLog>, val foods: List<FoodIdeaRecord>,
    val meals: List<MealLog>, val care: List<CareReminder>, val careLogs: List<CareLog>, val captures: List<Capture>,
    val observations: List<ActivityObservation>, val reflection: Reflection?, val notes: List<SessionNote> = emptyList())

class ExpandedRepository(private val provider: () -> SteadyDatabase, val clock: Clock,
    private val preferences: PreferencesRepository) {
    fun newId() = UUID.randomUUID().toString()
    private fun stableId(key: String) = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
    suspend fun logicalDay(): LocalDate = preferences.get().let {
        LogicalDayPolicy().getLogicalDay(clock.instant(), ZoneId.of(it.zoneId), it.boundaryMinutes)
    }
    suspend fun profile() = provider().expandedDao().profile() ?: ExpandedProfile()
    suspend fun saveProfile(value: ExpandedProfile) {
        PersonalizationRules.validate(value.ringMetrics,value.waterQuickMl,value.waterUnit,value.reviewCards)
        require(value.id == 1 && value.displayName.length <= 100 && value.country in setOf("IN", "OTHER"))
        require(value.palette in setOf("KINETIC", "DAYBOOK") && value.theme in setOf("SYSTEM", "LIGHT", "DARK"))
        require(value.textScale in 1f..2f && value.alertBudget in 0..5 && value.quietStart in 0..1439 && value.quietEnd in 0..1439)
        require(value.waterTargetMl == null || value.waterTargetMl > 0)
        require(value.weightKg == null || value.weightKg.isFinite() && value.weightKg in 1.0..1000.0)
        require(value.focusTargetMinutes == null || value.focusTargetMinutes in 1..1440)
        require(value.stepTarget == null || value.stepTarget > 0)
        val cards = value.dashboard.split(',').filter(String::isNotBlank)
        val supported = setOf("NEXT", "RINGS", "TIMELINE", "HABITS", "CAPTURE", "FOOD", "ROUTINES")
        require(cards.distinct().size == cards.size && cards.all { it in supported })
        require(value.wideCards.split(',').filter(String::isNotBlank).all { it in supported })
        require(value.modules.split(',').filter(String::isNotBlank).all { it in setOf("PLAN","HABITS","FOCUS","MOVEMENT","FOOD","WATER","SLEEP") })
        require(value.dateStyle in setOf("LOCAL","ISO") && value.zoneMode in setOf("FIXED","DEVICE"))
        provider().expandedDao().save(value)
    }
    suspend fun prepareDay(day: LocalDate) {
        val db = provider()
        db.withTransaction {
            val dao = db.expandedDao()
            if (dao.day(day.toString()) == null) {
                val p = db.preferencesDao().get() ?: AppPreferences()
                val old = db.dailyPlanDao().getPlan(day)
                dao.save(DaySettings(day.toString(), if (p.pauseEnabled) "PAUSED" else if (old?.mode == DayMode.MINIMUM) "MINIMUM" else "NORMAL",
                    old?.nextAction ?: "", old?.zoneId?.id ?: p.zoneId, old?.boundaryMinutes ?: p.boundaryMinutes,
                    old?.shutdownAt?.toEpochMilli(), clock.millis()))
                old?.let { plan ->
                    listOf("STUDY" to plan.studyTask, "BUILD" to plan.buildTask, "MOVEMENT" to plan.healthTask).forEachIndexed { order, (category, title) ->
                        if (title.isNotBlank()) dao.save(PlanItem(stableId("legacy-plan|$day|$category"), day.toString(), title,
                            notes = when(category) { "STUDY" -> plan.studyEvidence ?: ""; "BUILD" -> plan.buildEvidence ?: ""; else -> "" },
                            category = category, position = order, essential = true, created = plan.createdAt.toEpochMilli(),
                            updated = plan.updatedAt.toEpochMilli(), zone = plan.zoneId.id, boundary = plan.boundaryMinutes))
                    }
                }
            }
            dao.versionsForDay(day.toString()).forEach { version ->
                val id = stableId("habit|${version.habitId}|$day")
                if (dao.occurrence(id) == null && HabitRules.scheduled(day, LocalDate.parse(version.anchorDay), version.weekdays, version.everyDays))
                    dao.save(HabitOccurrence(id, version.habitId, version.id, day.toString(), updated = clock.millis()))
            }
        }
    }
    suspend fun snapshot(start: LocalDate, end: LocalDate, forExport: Boolean = false): PeriodSnapshot {
        require(!end.isBefore(start) && java.time.temporal.ChronoUnit.DAYS.between(start, end) <= 366)
        val db = provider()
        return db.withTransaction {
            val p = db.preferencesDao().get() ?: AppPreferences()
            val profile = db.expandedDao().profile() ?: ExpandedProfile()
            // Query a bounded UTC envelope, then interpret segments using their stored policy.
            // All supported zone offsets and a full logical-day shift fit inside two days.
            val begin = start.minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val stop = end.plusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
            val dao = db.expandedDao()
            val segments = dao.segmentsInRange(begin, stop)
            val relevantSessions = segments.filter { segment ->
                if (segment.endWall == null) false else {
                    val first = ActivityTotals.dayBounds(start,ZoneId.of(segment.zone),segment.boundary).start
                    val last = ActivityTotals.dayBounds(end,ZoneId.of(segment.zone),segment.boundary).end
                    segment.startWall < last && segment.endWall > first
                }
            }.mapTo(mutableSetOf()) { it.sessionId }
            val sessions = dao.sessions(begin, stop).filter { session ->
                val date = (session.ended ?: session.started)?.let { LogicalDayPolicy().getLogicalDay(Instant.ofEpochMilli(it), ZoneId.of(session.zone),session.boundary) }
                date != null && date in start..end || session.id in relevantSessions
            }
            PeriodSnapshot(start, end, profile, p, dao.days(start.toString(), end.toString()),
                dao.tasks(start.toString(), end.toString()), dao.habits(), dao.versionsForRange(start.toString(), end.toString()),
                dao.occurrences(start.toString(), end.toString()), sessions,
                segments, dao.activeSessions(), dao.subjects(),
                dao.water(start.toString(), end.toString()), dao.sleep(start.toString(), end.toString()), dao.foods(),
                dao.meals(start.toString(), end.toString()), dao.care(), dao.careLogs(start.toString(), end.toString()),
                if (forExport) dao.capturesInRange(begin, stop) else dao.captures(), dao.observations(start.toString(), end.toString()), dao.reflection(start.toString(), end.toString()),
                if (forExport) dao.notesInRange(begin, stop).filter { note ->
                    if (note.sessionId != null) sessions.any { it.id == note.sessionId }
                    else LogicalDayPolicy().getLogicalDay(Instant.ofEpochMilli(note.savedAt), ZoneId.of(p.zoneId), p.boundaryMinutes) in start..end
                } else emptyList())
        }
    }
    suspend fun observe(start: LocalDate, end: LocalDate): Flow<PeriodSnapshot> {
        val begin = start.minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val stop = end.plusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
        return provider().expandedDao().observeChanges(start.toString(), end.toString(), begin, stop).map { snapshot(start, end) }
    }
    suspend fun setDay(day: LocalDate, mode: String? = null, next: String? = null, shutdown: Boolean = false) {
        prepareDay(day)
        val db = provider()
        db.withTransaction {
            val old = db.expandedDao().day(day.toString())!!
            require(mode == null || mode in setOf("NORMAL", "MINIMUM", "PAUSED"))
            require(next == null || next.length <= 10_000)
            db.expandedDao().save(old.copy(mode = mode ?: old.mode, nextAction = next ?: old.nextAction,
                shutdown = if (shutdown) clock.millis() else old.shutdown, updated = clock.millis()))
        }
    }
    suspend fun saveTask(item: PlanItem) {
        UUID.fromString(item.id); LocalDate.parse(item.day); ZoneId.of(item.zone)
        require(item.title.isNotBlank() && item.title.length <= 500 && item.notes.length <= 100_000)
        require(item.boundary in 0..1439 && item.priority in 0..2 && item.state in setOf("PENDING", "COMPLETED", "ARCHIVED"))
        require(item.category in setOf("STUDY","BUILD","MOVEMENT","GENERAL","CARE","FOOD") && (item.projectId == null || item.projectId.length <= 200))
        require(item.plannedSeconds == null || item.plannedSeconds in 1..86_400)
        require(item.timeMinutes == null || item.timeMinutes in 0..1439)
        val db = provider()
        db.withTransaction {
            require(item.subjectId == null || db.expandedDao().subject(item.subjectId) != null)
            val existing = db.expandedDao().task(item.id)
            db.expandedDao().save(item.copy(created = existing?.created ?: item.created, updated = clock.millis()))
        }
    }
    suspend fun toggleTask(id: String) {
        val db = provider()
        db.withTransaction { val task = requireNotNull(db.expandedDao().task(id));
            db.expandedDao().save(task.copy(state = if (task.state == "COMPLETED") "PENDING" else "COMPLETED", updated = clock.millis())) }
    }
    suspend fun studyBlocks(day: LocalDate): List<StudyBlockProposal> = provider().expandedDao().notesWithPrefix("study-block:%")
        .map { Json.decodeFromString<StudyBlockProposal>(it.text).also { value -> value.validate(); require(it.id == "study-block:${value.id}") } }
        .filter { it.day == day.toString() }
    suspend fun studyBlockSettings(): StudyBlockSettings = provider().expandedDao().note("study-block-settings")
        ?.let { Json.decodeFromString<StudyBlockSettings>(it.text) } ?: StudyBlockSettings()
    suspend fun studyBlockSettings(value: StudyBlockSettings) = saveNote(SessionNote("study-block-settings", null, Json.encodeToString(value), clock.millis()))
    suspend fun studyBlockPlan(id: String): PlanItem? = provider().expandedDao().task(id)
    suspend fun saveStudyBlock(value: StudyBlockProposal) {
        value.validate()
        val db = provider()
        db.withTransaction {
            val dao = db.expandedDao()
            val old = dao.note("study-block:${value.id}")?.let { Json.decodeFromString<StudyBlockProposal>(it.text).also(StudyBlockProposal::validate) }
            check(value.revision == (old?.revision ?: 0) && value.adoptedPlanId == old?.adoptedPlanId)
            old?.adoptedPlanId?.let { id -> dao.task(id)?.let { task ->
                saveTask(task.copy(title = value.title, timeMinutes = value.minute,
                    plannedSeconds = value.durationMinutes * 60L, category = value.category, notes = value.primer,
                    zone = value.zone, boundary = value.boundary))
            } }
            dao.save(SessionNote("study-block:${value.id}", null, Json.encodeToString(value.copy(revision = value.revision + 1)), clock.millis()))
        }
    }
    suspend fun adoptStudyBlock(value: StudyBlockProposal): String {
        value.validate()
        val db = provider()
        return db.withTransaction {
            val dao = db.expandedDao()
            val stored = dao.note("study-block:${value.id}")?.let { Json.decodeFromString<StudyBlockProposal>(it.text).also(StudyBlockProposal::validate) }
            stored?.adoptedPlanId?.let { return@withTransaction it }
            check(value.revision == (stored?.revision ?: 0))
            val id = value.planId
            check(dao.task(id) == null)
            saveTask(PlanItem(id, value.day, value.title, notes = value.primer, category = value.category,
                plannedSeconds = value.durationMinutes * 60L, timeMinutes = value.minute,
                created = clock.millis(), updated = clock.millis(), zone = value.zone, boundary = value.boundary))
            dao.save(SessionNote("study-block:${value.id}", null, Json.encodeToString(value.copy(adoptedPlanId = id,
                revision = value.revision + 1)), clock.millis()))
            id
        }
    }
    suspend fun deleteTask(id: String) {
        val db = provider()
        db.withTransaction { db.expandedDao().detachTask(id); db.expandedDao().deleteTask(id) }
    }
    suspend fun saveHabit(version: HabitVersion) {
        UUID.fromString(version.id); UUID.fromString(version.habitId)
        LocalDate.parse(version.effectiveDay); LocalDate.parse(version.anchorDay)
        require(version.title.isNotBlank() && version.title.length <= 500 && version.unit.length <= 30)
        require(version.type in setOf("CHECKBOX", "COUNT", "DURATION", "QUANTITY"))
        require(version.target.isFinite() && version.target > 0 && version.weekdays in 1..127 && version.everyDays in 1..3650)
        require(version.reminderMinute == null || version.reminderMinute in 0..1439)
        require(!LocalDate.parse(version.effectiveDay).isBefore(logicalDay()))
        val db = provider()
        db.withTransaction {
            if (db.expandedDao().habit(version.habitId) == null) db.expandedDao().save(HabitDefinition(version.habitId, clock.millis()))
            // A logged occurrence retains its original snapshot. Same-day edits move to tomorrow.
            val logged = db.expandedDao().occurrences(version.effectiveDay, version.effectiveDay).any { it.habitId == version.habitId && it.state != "PENDING" }
            val effective = if (logged) LocalDate.parse(version.effectiveDay).plusDays(1).toString() else version.effectiveDay
            val existing = db.expandedDao().versionOn(version.habitId, effective)
            db.expandedDao().save(version.copy(id = existing?.id ?: version.id, effectiveDay = effective))
        }
        prepareDay(logicalDay())
    }
    suspend fun archiveHabit(id: String, day: LocalDate) {
        val db = provider()
        db.withTransaction { db.expandedDao().save(requireNotNull(db.expandedDao().habit(id)).copy(archivedDay = day.plusDays(1).toString())) }
    }
    suspend fun logHabit(occurrenceId: String, quantity: Double, eventId: String): HabitLog {
        require(quantity.isFinite() && quantity >= 0)
        UUID.fromString(eventId)
        val db = provider()
        return db.withTransaction {
            db.expandedDao().habitLog(eventId)?.let { return@withTransaction it }
            val old = requireNotNull(db.expandedDao().occurrence(occurrenceId))
            val version = db.expandedDao().versions(listOf(old.versionId)).single()
            val log = HabitLog(eventId, occurrenceId, quantity - old.quantity, clock.millis())
            db.expandedDao().save(log)
            db.expandedDao().save(old.copy(quantity = quantity, state = HabitRules.state(quantity, version.target), updated = clock.millis()))
            log
        }
    }
    suspend fun undoHabit(id: String) {
        val db = provider()
        db.withTransaction {
            val log = requireNotNull(db.expandedDao().habitLog(id))
            if (log.undoneAt != null) return@withTransaction
            val occurrence = requireNotNull(db.expandedDao().occurrence(log.occurrenceId))
            // Only the newest change may be undone; older edits cannot overwrite a subsequent correction.
            check(db.expandedDao().logs(log.occurrenceId).lastOrNull { it.undoneAt == null }?.id == id)
            val value = (occurrence.quantity - log.quantity).coerceAtLeast(0.0)
            val version = db.expandedDao().versions(listOf(occurrence.versionId)).single()
            db.expandedDao().save(log.copy(undoneAt = clock.millis()))
            db.expandedDao().save(occurrence.copy(quantity = value, state = HabitRules.state(value, version.target), updated = clock.millis()))
        }
    }
    suspend fun skipHabit(id: String) {
        val db = provider()
        db.withTransaction { val old = requireNotNull(db.expandedDao().occurrence(id));
            val version = db.expandedDao().versions(listOf(old.versionId)).single()
            db.expandedDao().save(old.copy(state = if (old.state == "SKIPPED") HabitRules.state(old.quantity, version.target) else "SKIPPED", updated = clock.millis())) }
    }
    suspend fun saveSubject(subject: Subject) {
        require(subject.title.isNotBlank() && subject.title.length <= 500 && subject.code.length <= 100)
        provider().expandedDao().save(subject)
    }
    suspend fun topics(subjectId: String) = provider().expandedDao().topics(subjectId)
    suspend fun saveTopic(topic: Topic) {
        require(topic.title.isNotBlank() && topic.title.length <= 500 && topic.state in setOf("NEW","ACTIVE","DONE"))
        val db = provider()
        db.withTransaction { require(db.expandedDao().subject(topic.subjectId) != null); db.expandedDao().save(topic) }
    }
    suspend fun habitNote(id: String, text: String) {
        require(text.length <= 100_000)
        val db = provider()
        db.withTransaction { val old = requireNotNull(db.expandedDao().occurrence(id)); db.expandedDao().save(old.copy(notes = text,updated = clock.millis())) }
    }
    suspend fun correctHabit(id: String, quantity: Double, note: String) {
        val db = provider()
        db.withTransaction { logHabit(id,quantity,newId()); habitNote(id,note) }
    }
    suspend fun saveNote(note: SessionNote) {
        require(note.text.length <= 100_000)
        val db = provider()
        db.withTransaction { require(note.sessionId == null || db.expandedDao().session(note.sessionId) != null)
            db.expandedDao().save(note.copy(savedAt = clock.millis())) }
    }
    suspend fun note(id: String) = provider().expandedDao().note(id)
    suspend fun deleteNote(id: String) = provider().expandedDao().deleteNote(id)
    suspend fun saveWater(log: WaterLog) { require(log.millilitres > 0); LocalDate.parse(log.day); provider().expandedDao().save(log) }
    suspend fun deleteWater(id: String) = provider().expandedDao().deleteWater(id)
    suspend fun saveSleep(log: SleepLog) {
        require(log.wake > log.bedtime && log.wake - log.bedtime <= 7 * 86_400_000L)
        require(log.restedness == null || log.restedness in 1..5)
        provider().expandedDao().save(log)
    }
    suspend fun deleteSleep(id: String) = provider().expandedDao().deleteSleep(id)
    suspend fun saveFood(food: FoodIdeaRecord) {
        require(food.title.isNotBlank() && food.title.length <= 500 && food.ingredients.length <= 100_000 && food.instructions.length <= 100_000)
        require(food.prepMinutes == null || food.prepMinutes in 0..1440)
        provider().expandedDao().save(food.copy(updated = clock.millis()))
    }
    suspend fun recipeContext(id: String): RecipeContext = provider().expandedDao().note("recipe-context:$id")
        ?.let { Json.decodeFromString<RecipeContext>(it.text).also(RecipeContext::validate) } ?: RecipeContext()
    suspend fun saveRecipe(food: FoodIdeaRecord, context: RecipeContext) {
        context.validate(); UUID.fromString(food.id)
        val db = provider()
        db.withTransaction {
            saveFood(food)
            db.expandedDao().save(SessionNote("recipe-context:${food.id}",null,Json.encodeToString(context),clock.millis()))
        }
    }
    suspend fun adoptMeal(value: MealPlanProposal): String {
        value.validate()
        val db = provider()
        return db.withTransaction {
            val dao = db.expandedDao()
            dao.note("meal-adoption:${value.id}")?.let { note ->
                val association = Json.decodeFromString<MealPlanAssociation>(note.text).also(MealPlanAssociation::validate)
                check(association.recipeId == value.recipeId)
                return@withTransaction association.planId
            }
            check(dao.food(value.recipeId) != null)
            check(dao.task(value.planId) == null)
            saveTask(PlanItem(value.planId,value.day,value.title,notes=value.notes,category="FOOD",timeMinutes=value.minute,
                plannedSeconds=value.minutes * 60L,zone=value.zone,boundary=value.boundary,created=clock.millis(),updated=clock.millis()))
            dao.save(SessionNote("meal-adoption:${value.id}",null,
                Json.encodeToString(MealPlanAssociation(value.id,value.recipeId,value.planId)),clock.millis()))
            value.planId
        }
    }
    suspend fun deleteFood(id: String) {
        val db = provider()
        db.withTransaction { db.expandedDao().detachFood(id); db.expandedDao().deleteNote("recipe-context:$id"); db.expandedDao().deleteFood(id) }
    }
    suspend fun saveMeal(log: MealLog) { require(log.title.isNotBlank()); provider().expandedDao().save(log) }
    suspend fun deleteMeal(id: String) = provider().expandedDao().deleteMeal(id)
    suspend fun saveCare(reminder: CareReminder) {
        require(reminder.instruction.isNotBlank() && reminder.instruction.length <= 10_000 && reminder.weekdays in 1..127 && reminder.minute in 0..1439)
        provider().expandedDao().save(reminder)
    }
    suspend fun deleteCare(id: String) {
        // Keep a disabled definition so historical care logs retain their relationship and snapshot.
        val dao = provider().expandedDao(); val old = dao.care().first { it.id == id }; dao.save(old.copy(enabled = false,updated = clock.millis()))
    }
    suspend fun reminderSettings(profile: ExpandedProfile, cues: Int, pauseNewDays: Boolean) {
        val db = provider()
        db.withTransaction {
            saveProfile(profile)
            preferences.update { it.copy(cueFlags = cues,pauseEnabled = pauseNewDays) }
        }
    }
    suspend fun saveCareLog(log: CareLog) = provider().expandedDao().save(log)
    suspend fun saveReflection(value: Reflection) {
        require(!LocalDate.parse(value.endDay).isBefore(LocalDate.parse(value.startDay)))
        require((value.mood == null || value.mood in 1..5) && (value.energy == null || value.energy in 1..5))
        require(listOf(value.helped, value.demanding, value.evidence, value.adjustment, value.highlight, value.obstacle, value.tomorrow).all { it.length <= 100_000 })
        val db = provider()
        db.withTransaction { val old = db.expandedDao().reflection(value.startDay, value.endDay)
            db.expandedDao().save(value.copy(id = old?.id ?: value.id, updated = clock.millis())) }
    }
    suspend fun saveCapture(value: Capture) { require(value.text.isNotBlank() && value.text.length <= 100_000); provider().expandedDao().save(value) }
    suspend fun interruption(value: InterruptionEvent, reason: String = "") {
        require(value.mode == "VOLUNTARY" && value.pauseSeconds in 0..60 && value.appPackage == null && value.outcome in setOf("RETURNED", "CONTINUED", "DISABLED"))
        require(reason.length <= 100_000)
        val db = provider()
        db.withTransaction {
            db.expandedDao().save(value)
            if (reason.isNotBlank()) db.expandedDao().save(Capture(value.id, reason, value.at))
        }
    }
    suspend fun interruptionCount(day: LocalDate): Int {
        val p = preferences.get()
        val bounds = ActivityTotals.dayBounds(day, ZoneId.of(p.zoneId), p.boundaryMinutes)
        return provider().expandedDao().interruptions(bounds.start, bounds.end - 1).size
    }
}
