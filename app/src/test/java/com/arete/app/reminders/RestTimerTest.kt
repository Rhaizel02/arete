package com.arete.app.reminders

import android.Manifest
import android.app.*
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.PreferencesRepository
import com.arete.app.data.local.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RestTimerTest {
    private lateinit var context: Context
    private lateinit var db: AreteDatabase
    private lateinit var prefs: PreferencesRepository
    private lateinit var scheduler: RestAlarmScheduler
    private lateinit var coordinator: RestTimerCoordinator
    private var clock = 1_800_000_000_000L
    private val deadline get() = clock + 10_000
    @Before fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        prefs = PreferencesRepository(context, com.arete.app.data.MemoryPreferences())
        prefs.setRestAlertsEnabled(true); prefs.setRestTimerEnabled(true); prefs.setVibrationEnabled(false)
        shadowOf(context.applicationContext as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler = RestAlarmScheduler(context)
        coordinator = RestTimerCoordinator(db, prefs, scheduler) { clock }
        db.sessionDao().insertSession(WorkoutSessionEntity("active", null, null, null, "Test", clock, restTimerEndAt = deadline, restTimerDurationSeconds = 10))
    }
    @After fun close() { scheduler.cancelAlarm(); scheduler.dismiss(); db.close() }
    private fun alarms() = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
    private fun notifications() = context.getSystemService(NotificationManager::class.java).activeNotifications

    @Test fun deniedExactUsesInexactAndGrantedUsesExactWithoutPermanentService() = runTest {
        assertEquals(RestDelivery.INEXACT, scheduler.schedule("active", deadline))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertEquals(RestDelivery.EXACT, scheduler.schedule("active", deadline))
        assertEquals(1, alarms().size)
        scheduler.cancelAlarm(); assertTrue(alarms().isEmpty())
    }
    @Test fun replacementAndSkipReconcileFromRoomNotIntent() = runTest {
        coordinator.reconcile(); assertEquals(1, alarms().size)
        db.sessionDao().startTimer("active", deadline + 15000, 25)
        coordinator.reconcile(); assertEquals(1, alarms().size)
        db.sessionDao().clearTimer("active")
        coordinator.reconcile(); assertTrue(alarms().isEmpty())
    }
    @Test fun oldChangedOrEarlyAlarmCannotConsumeCurrentRest() = runTest {
        val old = deadline
        db.sessionDao().startTimer("active", old + 15_000, 25)
        clock = old
        coordinator.elapsed("active", old, true)
        coordinator.elapsed("active", old + 15_000, true)
        assertEquals(old + 15_000, db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
        assertTrue(notifications().isEmpty())
    }
    @Test fun dueAlarmClaimsDeadlineOnceAndPostsCorrectNotification() = runTest {
        val due = deadline; clock = due
        coordinator.elapsed("active", due, true)
        assertNull(db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
        val shown = notifications().single()
        assertEquals("Descanso terminado", shown.notification.extras.getString("android.title"))
        assertNotNull(shown.notification.contentIntent)
        coordinator.elapsed("active", due, true)
        assertEquals(shown.postTime, notifications().single().postTime)
    }
    @Test fun foregroundFeedbackAndDeniedNotificationsDoNotDuplicateSystemAlerts() = runTest {
        coordinator.appVisible = true; coordinator.visibleSessionId = "active"
        val due = deadline; clock = due
        coordinator.elapsed("active", due, false)
        assertTrue(notifications().isEmpty())
        coordinator.appVisible = false
        shadowOf(context.applicationContext as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        db.sessionDao().startTimer("active", clock, 10)
        coordinator.elapsed("active", clock, true)
        assertTrue(notifications().isEmpty())
        assertNull(db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
    }
    @Test fun processRecreationReschedulesOnlyFutureAndDropsExpiredWithoutLateNotification() = runTest {
        RestTimerCoordinator(db, prefs, scheduler) { clock }.reconcile(true)
        assertEquals(1, alarms().size)
        clock += 20_000
        RestTimerCoordinator(db, prefs, scheduler) { clock }.reconcile(true)
        assertTrue(alarms().isEmpty()); assertTrue(notifications().isEmpty())
        assertNull(db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
    }
    @Test fun finishedDeletedOrDisabledCannotNotifyAndRestoredExpiredIsConsumed() = runTest {
        val due = deadline
        prefs.setRestAlertsEnabled(false); coordinator.reconcile()
        clock = due; coordinator.elapsed("active", due, true)
        assertTrue(notifications().isEmpty())
        prefs.setRestAlertsEnabled(true)
        coordinator.restoring { db.sessionDao().startTimer("active", clock - 1000, 10) }
        assertNull(db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
        db.sessionDao().startTimer("active", clock, 10)
        db.sessionDao().finishSession("active", clock)
        coordinator.elapsed("active", clock, true)
        db.sessionDao().deleteSession(db.sessionDao().getSession("active")!!.session)
        coordinator.elapsed("active", clock, true)
        assertTrue(notifications().isEmpty())
    }
    @Test fun excessivelyLateInexactDeliveryDoesNotInterruptTraining() = runTest {
        val due = deadline; clock = due + 120_001
        coordinator.elapsed("active", due, true)
        assertTrue(notifications().isEmpty())
        assertNull(db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
    }
    @Test fun receiverDoesNotWaitForASlowRestoreOrNotifyItsOldDeadline() = runTest {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val due = deadline; clock = due
        val restore = launch { coordinator.restoring { entered.complete(Unit); release.await() } }
        entered.await()
        coordinator.elapsed("active", due, true)
        assertTrue(notifications().isEmpty())
        assertEquals(due, db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
        release.complete(Unit); restore.join()
        assertNull(db.sessionDao().getActiveSession()!!.session.restTimerEndAt)
    }
}
