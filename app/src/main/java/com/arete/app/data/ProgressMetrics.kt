package com.arete.app.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.round

data class ProgressSet(
    val sessionId: String,
    val sessionStartedAt: Long,
    val sessionCompletedAt: Long,
    val sessionExerciseId: String,
    val exerciseId: String?,
    val exerciseName: String,
    val primaryMuscle: String?,
    val incrementKg: Double?,
    val position: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val isFailure: Boolean
)

/** Epley estimate. Reps above 12 are excluded because the estimate becomes less representative. */
object E1RmEstimator {
    const val MAX_REPRESENTATIVE_REPS = 12

    fun estimate(weightKg: Double?, reps: Int?): Double? {
        if (weightKg == null || weightKg <= 0.0 || !weightKg.isFinite()) return null
        if (reps == null || reps !in 1..MAX_REPRESENTATIVE_REPS) return null
        return weightKg * (1.0 + reps / 30.0)
    }
}

enum class PrType { MAX_WEIGHT, MAX_E1RM, REPS_AT_WEIGHT }

data class PersonalRecord(
    val type: PrType,
    val exerciseId: String,
    val exerciseName: String,
    val sessionId: String,
    val achievedAt: Long,
    val weightKg: Double,
    val reps: Int,
    val e1RmKg: Double?
)

data class ProgressionSuggestion(
    val exerciseId: String,
    val exerciseName: String,
    val currentWeightKg: Double,
    val suggestedWeightKg: Double,
    val incrementKg: Double,
    val achievedReps: Int,
    val targetReps: Int
)

data class WeeklyConsistency(
    val currentCount: Int,
    val goal: Int,
    val streak: Int,
    val recentWeeks: List<WeekActivity>
)

data class WeekActivity(val weekStart: Long, val workouts: Int)

data class BodyWeightTrend(
    val latestKg: Double?,
    val movingAverageKg: Double?,
    val recentChangeKg: Double?
)

object ProgressMetrics {
    /** Ephemeral: candidate must be saved as completed; confirmed history never includes an active session. */
    fun improvements(candidate: ProgressSet, confirmedHistory: List<ProgressSet>): List<PrType> {
        if (!isEffectiveWorkSet(candidate) || !validWeightAndReps(candidate) || candidate.exerciseId == null) return emptyList()
        val prior = confirmedHistory.filter {
            it.exerciseId == candidate.exerciseId && it.sessionId != candidate.sessionId &&
                it.sessionCompletedAt < candidate.sessionCompletedAt && isEffectiveWorkSet(it) && validWeightAndReps(it)
        }
        if (prior.isEmpty()) return emptyList()
        return recordsForSession(prior + candidate, candidate.sessionId).map { it.type }.filter { type ->
            when (type) {
                PrType.MAX_WEIGHT -> true
                PrType.MAX_E1RM -> prior.any { e1Rm(it) != null }
                PrType.REPS_AT_WEIGHT -> prior.any { weightKey(it.weightKg!!) == weightKey(candidate.weightKg!!) }
            }
        }
    }

    fun isEffectiveWorkSet(set: ProgressSet): Boolean =
        set.isCompleted && !set.isWarmup && (set.reps ?: 0) > 0

    fun e1Rm(set: ProgressSet): Double? = if (isEffectiveWorkSet(set)) {
        E1RmEstimator.estimate(set.weightKg, set.reps)
    } else null

    fun recordsForSession(allSets: List<ProgressSet>, sessionId: String): List<PersonalRecord> {
        val current = allSets.filter { it.sessionId == sessionId && isEffectiveWorkSet(it) && it.exerciseId != null }
        if (current.isEmpty()) return emptyList()
        val completedAt = current.maxOf { it.sessionCompletedAt }
        val output = mutableListOf<PersonalRecord>()
        current.groupBy { it.exerciseId!! }.forEach { (exerciseId, exerciseSets) ->
            val prior = allSets.filter {
                it.exerciseId == exerciseId && it.sessionCompletedAt < completedAt && isEffectiveWorkSet(it)
            }
            val weighted = exerciseSets.filter { validWeightAndReps(it) }
            if (weighted.isEmpty()) return@forEach

            val maxWeight = weighted.maxWithOrNull(compareBy<ProgressSet> { it.weightKg!! }.thenBy { it.reps!! })!!
            val priorMaxWeight = prior.mapNotNull { it.weightKg?.takeIf(Double::isFinite) }.maxOrNull()
            if (priorMaxWeight == null || maxWeight.weightKg!! > priorMaxWeight + EPSILON) {
                output += maxWeight.toRecord(PrType.MAX_WEIGHT)
            }

            val bestE1Rm = weighted.mapNotNull { set -> e1Rm(set)?.let { it to set } }.maxByOrNull { it.first }
            val priorBestE1Rm = prior.mapNotNull(::e1Rm).maxOrNull()
            if (bestE1Rm != null && (priorBestE1Rm == null || bestE1Rm.first > priorBestE1Rm + EPSILON)) {
                output += bestE1Rm.second.toRecord(PrType.MAX_E1RM)
            }

            val improvedRepSet = weighted.groupBy { weightKey(it.weightKg!!) }
                .mapNotNull { (weight, setsAtWeight) ->
                    val best = setsAtWeight.maxByOrNull { it.reps!! }!!
                    val priorReps = prior.filter { it.weightKg != null && weightKey(it.weightKg) == weight }
                        .mapNotNull { it.reps }.maxOrNull()
                    best.takeIf { priorReps == null || it.reps!! > priorReps }
                }
                .maxWithOrNull(compareBy<ProgressSet> { it.weightKg!! }.thenBy { it.reps!! })
            if (improvedRepSet != null) output += improvedRepSet.toRecord(PrType.REPS_AT_WEIGHT)
        }
        return output
    }

    fun allRecordEvents(allSets: List<ProgressSet>): List<PersonalRecord> =
        allSets.map { it.sessionId to it.sessionCompletedAt }.distinct().sortedBy { it.second }
            .flatMap { recordsForSession(allSets, it.first) }

    /**
     * Suggest one configured increment only when every work set in the latest prescription
     * reached its upper rep target at one load, without failure and without a reported RIR below 1.
     */
    fun progressionSuggestions(allSets: List<ProgressSet>): List<ProgressionSuggestion> =
        allSets.filter { it.exerciseId != null }
            .groupBy { it.exerciseId!! }
            .mapNotNull { (exerciseId, exerciseHistory) ->
                val latest = exerciseHistory.maxByOrNull { it.sessionCompletedAt } ?: return@mapNotNull null
                val rows = exerciseHistory.filter { it.sessionExerciseId == latest.sessionExerciseId && !it.isWarmup }
                val increment = latest.incrementKg?.takeIf { it > 0.0 && it.isFinite() } ?: return@mapNotNull null
                if (rows.isEmpty() || rows.any {
                        !it.isCompleted || (it.reps ?: 0) < it.targetRepsMax || it.isFailure ||
                            (it.rir != null && it.rir < 1) || it.weightKg == null || it.weightKg <= 0.0
                    }) return@mapNotNull null
                val load = rows.first().weightKg!!
                if (rows.any { abs(it.weightKg!! - load) > EPSILON }) return@mapNotNull null
                ProgressionSuggestion(
                    exerciseId = exerciseId,
                    exerciseName = latest.exerciseName,
                    currentWeightKg = load,
                    suggestedWeightKg = load + increment,
                    incrementKg = increment,
                    achievedReps = rows.minOf { it.reps!! },
                    targetReps = rows.maxOf { it.targetRepsMax }
                )
            }
            .sortedBy { it.exerciseName }

    fun weeklyConsistency(
        completedSessionTimes: List<Long>,
        goal: Int,
        now: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        weeksShown: Int = 8
    ): WeeklyConsistency {
        val safeGoal = goal.coerceAtLeast(1)
        val currentWeek = weekStart(now, timeZone)
        val counts = completedSessionTimes.groupingBy { weekStart(it, timeZone) }.eachCount()
        val recent = (weeksShown.coerceAtLeast(1) - 1 downTo 0).map { offset ->
            val start = shiftWeeks(currentWeek, -offset, timeZone)
            WeekActivity(start, counts[start] ?: 0)
        }
        var cursor = currentWeek
        if ((counts[cursor] ?: 0) < safeGoal) cursor = shiftWeeks(cursor, -1, timeZone)
        var streak = 0
        while ((counts[cursor] ?: 0) >= safeGoal) {
            streak++
            cursor = shiftWeeks(cursor, -1, timeZone)
        }
        return WeeklyConsistency(counts[currentWeek] ?: 0, safeGoal, streak, recent)
    }

    /** Monday 00:00 in the supplied local time zone. */
    fun weekStart(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
        return calendar.timeInMillis
    }

    fun bodyWeightTrend(weightsKg: List<Double>, window: Int = 7): BodyWeightTrend {
        val valid = weightsKg.filter { it > 0.0 && it.isFinite() }
        if (valid.isEmpty()) return BodyWeightTrend(null, null, null)
        val recent = valid.takeLast(window.coerceAtLeast(1))
        return BodyWeightTrend(
            latestKg = valid.last(),
            movingAverageKg = recent.average(),
            recentChangeKg = recent.takeIf { it.size >= 2 }?.let { it.last() - it.first() }
        )
    }

    private fun shiftWeeks(timestamp: Long, amount: Int, timeZone: TimeZone): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            add(Calendar.WEEK_OF_YEAR, amount)
        }.timeInMillis

    private fun validWeightAndReps(set: ProgressSet): Boolean =
        set.weightKg != null && set.weightKg > 0.0 && set.weightKg.isFinite() && (set.reps ?: 0) > 0

    private fun ProgressSet.toRecord(type: PrType) = PersonalRecord(
        type = type,
        exerciseId = exerciseId!!,
        exerciseName = exerciseName,
        sessionId = sessionId,
        achievedAt = sessionCompletedAt,
        weightKg = weightKg!!,
        reps = reps!!,
        e1RmKg = e1Rm(this)
    )

    private fun weightKey(weight: Double): Long = round(weight * 100.0).toLong()
    private const val EPSILON = 0.0001
}
