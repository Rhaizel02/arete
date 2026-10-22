package com.arete.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.AppSettings
import com.arete.app.data.WeeklyPlanEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutReminderSchedulerTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val scheduler = WorkoutReminderScheduler(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun selectsOnlyPlannedDaysAndRollsToNextWeekAfterTodaysTime() {
        val settings = AppSettings(
            remindersEnabled = true,
            reminderHour = 18,
            reminderMinute = 30,
            weeklyPlan = listOf(WeeklyPlanEntry(Calendar.MONDAY, "workout"))
        )
        assertEquals(
            epoch(2026, Calendar.SEPTEMBER, 7, 18, 30),
            scheduler.nextTriggerAt(settings, epoch(2026, Calendar.SEPTEMBER, 7, 17, 0), utc)
        )
        assertEquals(
            epoch(2026, Calendar.SEPTEMBER, 14, 18, 30),
            scheduler.nextTriggerAt(settings, epoch(2026, Calendar.SEPTEMBER, 7, 19, 0), utc)
        )
        assertNull(scheduler.nextTriggerAt(settings.copy(remindersEnabled = false), 1, utc))
    }

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
