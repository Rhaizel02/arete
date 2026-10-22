package com.arete.app.data

import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.BodyWeightEntryEntity
import com.arete.app.data.local.PerformanceSetRow
import com.arete.app.data.local.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

data class ExerciseSessionPoint(
    val sessionId: String,
    val completedAt: Long,
    val maxWeightKg: Double?,
    val bestWeightKg: Double?,
    val bestReps: Int?,
    val bestE1RmKg: Double?
)

data class ExerciseProgress(
    val exerciseId: String,
    val exerciseName: String,
    val primaryMuscle: String?,
    val sessionCount: Int,
    val lastPerformedAt: Long,
    val maxWeightKg: Double?,
    val bestWeightKg: Double?,
    val bestReps: Int?,
    val bestE1RmKg: Double?,
    val recentE1RmChangeKg: Double?,
    val history: List<ExerciseSessionPoint>,
    val records: List<PersonalRecord>
)

data class MuscleSetCount(val muscle: String, val sets: Int)
data class MonthActivity(val monthStart: Long, val workouts: Int)

data class GeneralStats(
    val totalWorkouts: Int = 0,
    val workoutsPerWeek: Double = 0.0,
    val averageDurationMinutes: Int? = null,
    val effectiveWorkSets: Int = 0,
    val muscleDistribution: List<MuscleSetCount> = emptyList(),
    val monthlyActivity: List<MonthActivity> = emptyList()
)

data class ProgressDashboard(
    val weekly: WeeklyConsistency = WeeklyConsistency(0, 3, 0, emptyList()),
    val exercises: List<ExerciseProgress> = emptyList(),
    val stats: GeneralStats = GeneralStats(),
    val latestRecord: PersonalRecord? = null,
    val suggestions: List<ProgressionSuggestion> = emptyList(),
    val bodyWeights: List<BodyWeightEntryEntity> = emptyList(),
    val bodyWeightTrend: BodyWeightTrend = BodyWeightTrend(null, null, null)
)

class ProgressRepository(
    private val database: AreteDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val dao = database.progressDao()

    fun dashboard(weeklyGoal: Flow<Int>): Flow<ProgressDashboard> = combine(
        dao.observePerformanceSets(),
        database.sessionDao().observeCompletedSessionHeaders(),
        dao.observeBodyWeights(),
        weeklyGoal,
        PlanningDates.dayChanges(now)
    ) { rows, sessions, weights, goal, _ ->
        buildDashboard(rows.map(::toProgressSet), sessions, weights, goal)
    }

    fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> =
        dao.observePerformanceSets().map { rows ->
            val sets = rows.map(::toProgressSet)
            buildExerciseProgress(sets, ProgressMetrics.allRecordEvents(sets))
                .firstOrNull { it.exerciseId == exerciseId }
        }

    suspend fun recordsForSession(sessionId: String): List<PersonalRecord> {
        val all = dao.observePerformanceSets().first().map(::toProgressSet)
        return ProgressMetrics.recordsForSession(all, sessionId)
    }

    suspend fun saveBodyWeight(
        id: String?,
        weightKg: Double,
        notes: String = "",
        recordedAt: Long? = null
    ): String {
        require(weightKg in 1.0..1_000.0 && weightKg.isFinite()) { "Introduce un peso válido" }
        val existing = id?.let { dao.getBodyWeight(it) }
        val targetDate = recordedAt ?: existing?.recordedAt ?: now()
        require(targetDate > 0) { "Selecciona una fecha válida" }
        val sameDate = dao.getBodyWeightAt(targetDate)
        require(sameDate == null || sameDate.id == existing?.id) { "Ya existe un registro en esa fecha" }
        val entryId = existing?.id ?: newId()
        dao.upsertBodyWeight(
            BodyWeightEntryEntity(
                id = entryId,
                weightKg = weightKg,
                recordedAt = targetDate,
                notes = notes.trim()
            )
        )
        return entryId
    }

    suspend fun deleteBodyWeight(entry: BodyWeightEntryEntity) = dao.deleteBodyWeight(entry)

    internal fun buildDashboard(
        sets: List<ProgressSet>,
        sessions: List<WorkoutSessionEntity>,
        weights: List<BodyWeightEntryEntity>,
        goal: Int
    ): ProgressDashboard {
        val recordEvents = ProgressMetrics.allRecordEvents(sets)
        val weekly = ProgressMetrics.weeklyConsistency(
            sessions.mapNotNull { it.completedAt }, goal, now()
        )
        return ProgressDashboard(
            weekly = weekly,
            exercises = buildExerciseProgress(sets, recordEvents),
            stats = buildStats(sets, sessions, weekly),
            latestRecord = recordEvents.maxWithOrNull(
                compareBy<PersonalRecord> { it.achievedAt }.thenBy { recordPriority(it.type) }
            ),
            suggestions = ProgressMetrics.progressionSuggestions(sets),
            bodyWeights = weights.sortedBy { it.recordedAt },
            bodyWeightTrend = ProgressMetrics.bodyWeightTrend(weights.sortedBy { it.recordedAt }.map { it.weightKg })
        )
    }

    private fun buildExerciseProgress(
        allSets: List<ProgressSet>,
        records: List<PersonalRecord>
    ): List<ExerciseProgress> = allSets.filter { it.exerciseId != null }
        .groupBy { it.exerciseId!! }
        .mapNotNull { (exerciseId, rows) ->
            val effective = rows.filter(ProgressMetrics::isEffectiveWorkSet)
            if (effective.isEmpty()) return@mapNotNull null
            val points = effective.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
                val best = bestSet(sessionSets)
                ExerciseSessionPoint(
                    sessionId = sessionId,
                    completedAt = sessionSets.maxOf { it.sessionCompletedAt },
                    maxWeightKg = sessionSets.mapNotNull { it.weightKg?.takeIf { kg -> kg > 0.0 } }.maxOrNull(),
                    bestWeightKg = best?.weightKg,
                    bestReps = best?.reps,
                    bestE1RmKg = sessionSets.mapNotNull(ProgressMetrics::e1Rm).maxOrNull()
                )
            }.sortedBy { it.completedAt }
            val best = bestSet(effective)
            val e1rms = points.mapNotNull { it.bestE1RmKg }
            ExerciseProgress(
                exerciseId = exerciseId,
                exerciseName = rows.maxByOrNull { it.sessionCompletedAt }!!.exerciseName,
                primaryMuscle = rows.mapNotNull { it.primaryMuscle }.lastOrNull(),
                sessionCount = points.size,
                lastPerformedAt = points.last().completedAt,
                maxWeightKg = effective.mapNotNull { it.weightKg?.takeIf { kg -> kg > 0.0 } }.maxOrNull(),
                bestWeightKg = best?.weightKg,
                bestReps = best?.reps,
                bestE1RmKg = e1rms.maxOrNull(),
                recentE1RmChangeKg = e1rms.takeLast(2).takeIf { it.size == 2 }?.let { it[1] - it[0] },
                history = points.asReversed(),
                records = records.filter { it.exerciseId == exerciseId }.groupBy { it.type }
                    .mapNotNull { (_, typeRecords) -> typeRecords.maxByOrNull { it.achievedAt } }
                    .sortedBy { recordPriority(it.type) }
            )
        }
        .sortedByDescending { it.lastPerformedAt }

    private fun buildStats(
        sets: List<ProgressSet>,
        sessions: List<WorkoutSessionEntity>,
        weekly: WeeklyConsistency
    ): GeneralStats {
        val effective = sets.filter(ProgressMetrics::isEffectiveWorkSet)
        val reliableDurations = sessions.mapNotNull { session ->
            session.completedAt?.minus(session.startedAt)?.takeIf { it in 60_000L..21_600_000L }
        }
        val muscles = effective.groupingBy { it.primaryMuscle?.takeIf(String::isNotBlank) ?: "Sin clasificar" }
            .eachCount().map { MuscleSetCount(it.key, it.value) }.sortedByDescending { it.sets }
        val monthly = sessions.mapNotNull { it.completedAt }.groupingBy(::monthStart).eachCount()
            .map { MonthActivity(it.key, it.value) }.sortedBy { it.monthStart }.takeLast(6)
        return GeneralStats(
            totalWorkouts = sessions.size,
            workoutsPerWeek = weekly.recentWeeks.map { it.workouts }.averageOrZero(),
            averageDurationMinutes = reliableDurations.takeIf { it.isNotEmpty() }
                ?.average()?.div(60_000.0)?.toInt(),
            effectiveWorkSets = effective.size,
            muscleDistribution = muscles,
            monthlyActivity = monthly
        )
    }

    private fun bestSet(sets: List<ProgressSet>): ProgressSet? = sets.maxWithOrNull(
        compareBy<ProgressSet> { ProgressMetrics.e1Rm(it) ?: -1.0 }
            .thenBy { it.weightKg ?: -1.0 }
            .thenBy { it.reps ?: -1 }
    )

    private fun monthStart(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private fun recordPriority(type: PrType): Int = when (type) {
        PrType.MAX_E1RM -> 3
        PrType.MAX_WEIGHT -> 2
        PrType.REPS_AT_WEIGHT -> 1
    }

    private fun toProgressSet(row: PerformanceSetRow) = ProgressSet(
        sessionId = row.sessionId,
        sessionStartedAt = row.sessionStartedAt,
        sessionCompletedAt = row.sessionCompletedAt,
        sessionExerciseId = row.sessionExerciseId,
        exerciseId = row.exerciseId,
        exerciseName = row.exerciseName,
        primaryMuscle = row.primaryMuscle,
        incrementKg = row.incrementKg,
        position = row.setPosition,
        targetRepsMin = row.targetRepsMin,
        targetRepsMax = row.targetRepsMax,
        isWarmup = row.isWarmup,
        isCompleted = row.isCompleted,
        weightKg = row.weightKg,
        reps = row.reps,
        rir = row.rir,
        isFailure = row.isFailure
    )
}
