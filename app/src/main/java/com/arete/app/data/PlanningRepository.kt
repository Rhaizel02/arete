package com.arete.app.data

import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.SkippedPlannedWorkoutEntity
import com.arete.app.data.local.WorkoutEntity
import com.arete.app.data.local.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

enum class ActivityDayStatus { COMPLETED, PLANNED, SKIPPED, NONE }

data class ActivityDay(
    val timestamp: Long,
    val epochDay: Long,
    val status: ActivityDayStatus,
    val plannedWorkoutName: String? = null,
    val completedSessions: List<WorkoutSessionEntity> = emptyList(),
    val skipped: SkippedPlannedWorkoutEntity? = null
)

data class TodayPlan(
    val epochDay: Long,
    val plannedWorkout: WorkoutEntity?,
    val completedSession: WorkoutSessionEntity?,
    val skipped: SkippedPlannedWorkoutEntity?
)

data class PlanningState(
    val routineName: String? = null,
    val workouts: List<WorkoutEntity> = emptyList(),
    val weeklyPlan: List<WeeklyPlanEntry> = emptyList(),
    val today: TodayPlan = TodayPlan(0, null, null, null),
    val activity: List<ActivityDay> = emptyList()
)

class PlanningRepository(
    private val database: AreteDatabase,
    private val preferences: PreferencesRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val dao = database.planningDao()

    val state: Flow<PlanningState> = combine(
        database.routineDao().observeActiveRoutine(),
        database.sessionDao().observeCompletedSessionHeaders(),
        dao.observeSkippedDays(),
        preferences.weeklyPlan,
        PlanningDates.dayChanges(now)
    ) { routine, sessions, skipped, plan, _ ->
        buildState(
            routineName = routine?.routine?.name,
            workouts = routine?.orderedWorkouts()?.map { it.workout }.orEmpty(),
            sessions = sessions,
            skipped = skipped,
            plan = plan,
            currentTime = now(),
            zone = timeZone()
        )
    }

    suspend fun setPlannedWorkout(dayOfWeek: Int, workoutId: String?) {
        if (workoutId != null) {
            val active = database.routineDao().observeActiveRoutine().first()
            require(active?.orderedWorkouts()?.any { it.workout.id == workoutId } == true) {
                "El workout no pertenece a la rutina activa"
            }
        }
        preferences.setPlannedWorkout(dayOfWeek, workoutId)
    }

    suspend fun skipToday(): SkippedPlannedWorkoutEntity {
        val current = state.first().today
        val workout = current.plannedWorkout ?: throw IllegalStateException("Hoy no hay entrenamiento planificado")
        current.skipped?.let { return it }
        check(current.completedSession == null) { "El entrenamiento de hoy ya está completado" }
        val routine = database.routineDao().observeActiveRoutine().first()?.routine
        val entry = SkippedPlannedWorkoutEntity(
            id = newId(),
            routineId = routine?.id,
            workoutId = workout.id,
            routineNameSnapshot = routine?.name,
            workoutNameSnapshot = workout.name,
            plannedEpochDay = current.epochDay,
            skippedAt = now()
        )
        dao.insertSkippedDay(entry)
        return entry
    }

    suspend fun undoSkippedDay(entry: SkippedPlannedWorkoutEntity) = dao.deleteSkippedDay(entry)

    suspend fun shouldSendReminderNow(): Boolean {
        val settings = preferences.settings.first()
        if (!settings.remindersEnabled || settings.weeklyPlan.none { it.dayOfWeek == PlanningDates.dayOfWeek(now(), timeZone()) }) return false
        val current = state.first().today
        return current.plannedWorkout != null && current.completedSession == null && current.skipped == null &&
            database.sessionDao().getActiveSession() == null
    }

    internal fun buildState(
        routineName: String?,
        workouts: List<WorkoutEntity>,
        sessions: List<WorkoutSessionEntity>,
        skipped: List<SkippedPlannedWorkoutEntity>,
        plan: List<WeeklyPlanEntry>,
        currentTime: Long,
        zone: TimeZone
    ): PlanningState {
        val workoutById = workouts.associateBy { it.id }
        val validPlan = plan.filter { workoutById.containsKey(it.workoutId) }.distinctBy { it.dayOfWeek }
        val planByDay = validPlan.associateBy { it.dayOfWeek }
        val sessionsByDay = sessions.filter { it.completedAt != null }.groupBy { PlanningDates.epochDay(it.completedAt!!, zone) }
        val skippedByDay = skipped.groupBy { it.plannedEpochDay }
        val todayEpoch = PlanningDates.epochDay(currentTime, zone)
        val activity = (-13..7).map { offset ->
            val timestamp = PlanningDates.shiftDays(currentTime, offset, zone)
            val epochDay = PlanningDates.epochDay(timestamp, zone)
            val completed = sessionsByDay[epochDay].orEmpty().sortedByDescending { it.completedAt }
            val skip = skippedByDay[epochDay]?.maxByOrNull { it.skippedAt }
            val planned = planByDay[PlanningDates.dayOfWeek(timestamp, zone)]?.let { workoutById[it.workoutId] }
            val status = when {
                completed.isNotEmpty() -> ActivityDayStatus.COMPLETED
                skip != null -> ActivityDayStatus.SKIPPED
                planned != null -> ActivityDayStatus.PLANNED
                else -> ActivityDayStatus.NONE
            }
            ActivityDay(timestamp, epochDay, status, planned?.name, completed, skip)
        }
        val todayActivity = activity.first { it.epochDay == todayEpoch }
        val todayWorkout = planByDay[PlanningDates.dayOfWeek(currentTime, zone)]?.let { workoutById[it.workoutId] }
        return PlanningState(
            routineName = routineName,
            workouts = workouts,
            weeklyPlan = validPlan,
            today = TodayPlan(todayEpoch, todayWorkout, todayActivity.completedSessions.firstOrNull(), todayActivity.skipped),
            activity = activity
        )
    }
}

object PlanningDates {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun dayChanges(now: () -> Long = System::currentTimeMillis): Flow<Long> = flow {
        while (true) {
            emit(epochDay(now()))
            delay(60_000)
        }
    }.distinctUntilChanged()

    fun dayOfWeek(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(zone).apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK)

    /** Stable date key: the local calendar date encoded as a UTC epoch-day. */
    fun epochDay(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis / MILLIS_PER_DAY
    }

    fun shiftDays(timestamp: Long, amount: Int, zone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(zone).apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_MONTH, amount)
        }.timeInMillis

    fun pickerDate(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis
    }

    fun timestampFromPickerDate(pickerUtcMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerUtcMillis }
        return Calendar.getInstance(zone).apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        }.timeInMillis
    }
}
