package com.arete.app.data

import java.util.Calendar
import java.util.TimeZone

enum class ProgressPeriod(val months: Long?, val label: String) {
    MONTH(1, "1 mes"), QUARTER(3, "3 meses"), HALF_YEAR(6, "6 meses"), ALL(null, "Todo");

    /** Inclusive local midnight, calendar months back; the upper bound is the current instant. */
    fun start(now: Long, zone: TimeZone = TimeZone.getDefault()): Long? = months?.let {
        Calendar.getInstance(zone).apply {
            timeInMillis = now
            add(Calendar.MONTH, -it.toInt())
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun contains(timestamp: Long, now: Long, zone: TimeZone = TimeZone.getDefault()): Boolean =
        timestamp <= now && (start(now, zone)?.let { timestamp >= it } ?: true)

    fun weeks(now: Long, earliest: Long?, zone: TimeZone = TimeZone.getDefault()): Double {
        val first = start(now, zone) ?: earliest ?: now
        fun civilDay(timestamp: Long): Long {
            val local = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
            return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis / 86_400_000L
        }
        val days = civilDay(now) - civilDay(first) + 1
        return days.coerceAtLeast(1) / 7.0
    }
}

data class PerformanceDelta(val amount: Double, val percent: Double?)

fun performanceDelta(current: Double?, baseline: Double?): PerformanceDelta? {
    if (current == null || baseline == null || !current.isFinite() || !baseline.isFinite()) return null
    val amount = current - baseline
    return PerformanceDelta(amount, baseline.takeIf { it > 0 }?.let { amount / it * 100 })
}

/** Only consecutive sessions with a representative estimate can be compared. */
fun sessionComparison(history: List<ExerciseSessionPoint>): PerformanceDelta? {
    val latest = history.sortedByDescending { it.completedAt }.take(2)
    if (latest.size != 2) return null
    return performanceDelta(latest[0].bestE1RmKg, latest[1].bestE1RmKg)
}

fun periodComparison(history: List<ExerciseSessionPoint>): PerformanceDelta? {
    val valid = history.filter { it.bestE1RmKg != null }.sortedBy { it.completedAt }
    if (valid.size < 2) return null
    return performanceDelta(valid.last().bestE1RmKg, valid.first().bestE1RmKg)
}
