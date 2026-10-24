package com.arete.app.data

import java.util.Calendar
import java.util.TimeZone

data class VolumeTotals(val externalKgReps: Double? = null, val effectiveSets: Int = 0,
    val completedReps: Long = 0, val knownLoadSets: Int = 0)
data class VolumePoint(val id: String, val label: String, val at: Long, val totals: VolumeTotals)
data class ExternalVolume(val totals: VolumeTotals = VolumeTotals(), val sessions: List<VolumePoint> = emptyList(),
    val exercises: List<VolumePoint> = emptyList(), val muscles: List<VolumePoint> = emptyList(),
    val weeks: List<VolumePoint> = emptyList())

object ExternalVolumeMetrics {
    fun completeWeekChange(volume: ExternalVolume, period: ProgressPeriod, now: Long, zone: TimeZone = TimeZone.getDefault()): PerformanceDelta? {
        val cursor = Calendar.getInstance(zone).apply { timeInMillis = ProgressMetrics.weekStart(now, zone); add(Calendar.WEEK_OF_YEAR, -1) }
        val latestWeek = cursor.timeInMillis
        cursor.add(Calendar.WEEK_OF_YEAR, -1)
        val priorWeek = cursor.timeInMillis
        if (period.start(now, zone)?.let { it > priorWeek } == true) return null
        return performanceDelta(volume.weeks.firstOrNull { it.at == latestWeek }?.totals?.externalKgReps,
            volume.weeks.firstOrNull { it.at == priorWeek }?.totals?.externalKgReps)
    }
    fun load(set: ProgressSet): Double? {
        if (!ProgressMetrics.isEffectiveWorkSet(set)) return null
        val kg = set.weightKg?.takeIf { it > 0 && it.isFinite() } ?: return null
        return (kg * set.reps!!).takeIf { it.isFinite() }
    }
    fun totals(sets: List<ProgressSet>): VolumeTotals {
        val work = sets.filter(ProgressMetrics::isEffectiveWorkSet)
        val known = work.mapNotNull(::load)
        return VolumeTotals(known.takeIf { it.isNotEmpty() }?.sum()?.takeIf { it.isFinite() }, work.size,
            work.sumOf { it.reps!!.toLong() }, known.size)
    }
    /** Input is the single joined completed-session read, not active sessions. Empty weeks keep null (no records). */
    fun calculate(sets: List<ProgressSet>, period: ProgressPeriod, now: Long, zone: TimeZone = TimeZone.getDefault()): ExternalVolume {
        val filtered = sets.filter { period.contains(it.sessionCompletedAt, now, zone) }
        fun points(key: (ProgressSet) -> String, label: (ProgressSet) -> String) = filtered.groupBy(key).map { (id, rows) ->
            VolumePoint(id, label(rows.maxBy { it.sessionCompletedAt }), rows.maxOf { it.sessionCompletedAt }, totals(rows))
        }
        val byWeek = filtered.groupBy { ProgressMetrics.weekStart(it.sessionCompletedAt, zone) }
        val weeks = mutableListOf<VolumePoint>()
        val first = period.start(now, zone) ?: filtered.minOfOrNull { it.sessionCompletedAt }
        if (first != null) {
            val cursor = Calendar.getInstance(zone).apply { timeInMillis = ProgressMetrics.weekStart(first, zone) }
            val last = ProgressMetrics.weekStart(now, zone)
            while (cursor.timeInMillis <= last) {
                val at = cursor.timeInMillis
                weeks += VolumePoint(at.toString(), "", at, totals(byWeek[at].orEmpty()))
                cursor.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        return ExternalVolume(totals(filtered), points({ it.sessionId }, { it.sessionId }).sortedBy { it.at },
            points({ it.exerciseId ?: "unlinked:${it.sessionExerciseId}" }, { it.exerciseName }).sortedByDescending { it.at },
            points({ it.primaryMuscle?.trim()?.takeIf(String::isNotEmpty) ?: "Sin clasificar" },
                { it.primaryMuscle?.trim()?.takeIf(String::isNotEmpty) ?: "Sin clasificar" }).sortedByDescending { it.totals.externalKgReps ?: -1.0 }, weeks)
    }
}
