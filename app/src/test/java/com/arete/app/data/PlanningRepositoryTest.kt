package com.arete.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.WorkoutEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlanningRepositoryTest {
    private lateinit var database: AreteDatabase
    private lateinit var preferences: PreferencesRepository
    private lateinit var planning: PlanningRepository
    private lateinit var sessions: SessionRepository
    private val utc = TimeZone.getTimeZone("UTC")
    private val monday = epoch(2026, Calendar.SEPTEMBER, 7, 10)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        preferences = PreferencesRepository(context)
        preferences.restore(BackupPreferences())
        planning = PlanningRepository(database, preferences, now = { monday }, timeZone = { utc }, newId = { "skip" })
        sessions = SessionRepository(database, now = { monday }, newId = { "generated" })
        database.routineDao().insertRoutine(RoutineEntity("routine", "Torso/Pierna", true, 1, 1))
        database.routineDao().insertWorkout(WorkoutEntity("a", "routine", "Torso A", 0, createdAt = 1, updatedAt = 1))
        database.routineDao().insertWorkout(WorkoutEntity("b", "routine", "Pierna A", 1, createdAt = 1, updatedAt = 1))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun skippingTodayIsVisibleUndoableAndNeverCreatesOrAdvancesASession() = runTest {
        planning.setPlannedWorkout(Calendar.MONDAY, "a")
        val nextBefore = sessions.homeState.first().nextWorkout!!.workoutId
        val skipped = planning.skipToday()
        preferences.setRemindersEnabled(true)

        assertEquals("a", skipped.workoutId)
        assertNotNull(planning.state.first().today.skipped)
        assertNull(database.sessionDao().getActiveSession())
        assertTrue(database.sessionDao().observeCompletedSessionHeaders().first().isEmpty())
        assertEquals(nextBefore, sessions.homeState.first().nextWorkout!!.workoutId)
        assertFalse(planning.shouldSendReminderNow())

        planning.undoSkippedDay(skipped)
        assertNull(planning.state.first().today.skipped)
        assertEquals(ActivityDayStatus.PLANNED, planning.state.first().activity.first { it.epochDay == PlanningDates.epochDay(monday, utc) }.status)
    }

    @Test
    fun reminderIsSuppressedForActiveOrAlreadyCompletedTraining() = runTest {
        planning.setPlannedWorkout(Calendar.MONDAY, "a")
        preferences.setRemindersEnabled(true)
        assertTrue(planning.shouldSendReminderNow())
        val sessionId = sessions.startFreeWorkout()
        assertFalse(planning.shouldSendReminderNow())
        sessions.finishSession(database.sessionDao().getSession(sessionId)!!.session)
        assertFalse(planning.shouldSendReminderNow())
    }

    @Test
    fun dateKeysRespectLocalDaysAndActivityDistinguishesEveryState() = runTest {
        val sunday = epoch(2026, Calendar.SEPTEMBER, 6, 23)
        assertEquals(Calendar.SUNDAY, PlanningDates.dayOfWeek(sunday, utc))
        assertEquals(PlanningDates.epochDay(sunday, utc) + 1, PlanningDates.epochDay(monday, utc))
        val picker = PlanningDates.pickerDate(monday, utc)
        assertEquals(PlanningDates.epochDay(monday, utc), PlanningDates.epochDay(PlanningDates.timestampFromPickerDate(picker, utc), utc))

        planning.setPlannedWorkout(Calendar.MONDAY, "a")
        val state = planning.state.first()
        assertEquals(ActivityDayStatus.PLANNED, state.activity.first { it.epochDay == state.today.epochDay }.status)
        assertTrue(state.activity.any { it.status == ActivityDayStatus.NONE })
    }

    private fun epoch(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
