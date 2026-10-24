package com.arete.app.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.arete.app.AreteApplication
import com.arete.app.MainActivity
import com.arete.app.R
import com.arete.app.data.PreferencesRepository
import com.arete.app.data.local.AreteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class RestDelivery { EXACT, INEXACT }

/** No ticking service: just one replaceable PendingIntent referring to the Room deadline. */
class RestAlarmScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    fun notificationsAllowed() = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun schedule(id: String, deadline: Long): RestDelivery {
        cancelAlarm()
        val pending = pending(id, deadline)
        if (exactAllowed()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pending)
                return RestDelivery.EXACT
            } catch (_: SecurityException) { /* Permission changed: degrade without blocking training. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pending)
        return RestDelivery.INEXACT
    }
    fun cancelAlarm() { alarms.cancel(pending("", 0)) }
    fun dismiss() { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    private fun pending(id: String, deadline: Long) = PendingIntent.getBroadcast(context, 910,
        Intent(context, RestAlarmReceiver::class.java).setAction(ACTION_REST)
            .putExtra("sessionId", id).putExtra("deadline", deadline),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun feedback(vibration: Boolean, visible: Boolean) {
        if (visible) {
            if (vibration) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(150)
            }
            return
        }
        if (!notificationsAllowed()) return
        val channel = if (vibration) "rest_done_vibration" else "rest_done_silent"
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channel, if (vibration) "Fin de descanso" else "Fin de descanso sin vibración", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(vibration); setSound(null, null)
            })
        val open = PendingIntent.getActivity(context, 911,
            Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_SESSION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID,
            NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Descanso terminado").setContentText("Puedes continuar con tu entrenamiento")
                .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).build())
        } catch (_: SecurityException) { /* Revocation does not affect the saved session. */ }
    }
    companion object {
        const val ACTION_REST = "com.arete.app.REST_DONE"
        const val ACTION_OPEN_SESSION = "com.arete.app.OPEN_ACTIVE_SESSION"
        const val NOTIFICATION_ID = 910
    }
}

/** Same instance in the UI and receiver. Room CAS is also idempotent across process recreation. */
class RestTimerCoordinator(
    private val database: AreteDatabase, private val preferences: PreferencesRepository,
    private val scheduler: RestAlarmScheduler, private val now: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private var restoresInFlight = 0
    @Volatile var appVisible = false
    @Volatile var visibleSessionId: String? = null

    suspend fun reconcile(dropExpired: Boolean = false) = mutex.withLock {
        if (restoresInFlight == 0) reconcileLocked(dropExpired)
    }
    suspend fun restoring(block: suspend () -> Unit) {
        mutex.withLock { restoresInFlight++; scheduler.cancelAlarm(); scheduler.dismiss() }
        // A slow SAF backup/restore must not keep a BroadcastReceiver waiting on our mutex.
        try { block() } finally {
            withContext(NonCancellable) { mutex.withLock {
                restoresInFlight--
                if (restoresInFlight == 0) reconcileLocked(dropExpired = true)
            } }
        }
    }
    private suspend fun reconcileLocked(dropExpired: Boolean) {
        val active = database.sessionDao().getActiveSession()?.session
        val settings = preferences.settings.first()
        scheduler.cancelAlarm()
        val deadline = active?.restTimerEndAt
        if (active == null || !settings.restAlertsEnabled || !settings.restTimerEnabled) scheduler.dismiss()
        if (active != null && deadline != null) {
            if (deadline > now() && settings.restAlertsEnabled && settings.restTimerEnabled) {
                scheduler.dismiss()
                scheduler.schedule(active.id, deadline)
            } else if (deadline <= now() && dropExpired) {
                database.sessionDao().consumeTimer(active.id, deadline)
            }
        }
    }

    suspend fun elapsed(sessionId: String, deadline: Long, fromAlarm: Boolean) = mutex.withLock {
        if (restoresInFlight > 0) return@withLock
        val settings = preferences.settings.first()
        database.withTransaction {
            val active = database.sessionDao().getActiveSession()?.session ?: return@withTransaction
            if (active.id != sessionId || active.restTimerEndAt != deadline || now() < deadline) return@withTransaction
            // Inexact alarms can be delayed; only a short grace is useful, never an old rest on app launch.
            val timely = now() - deadline <= 120_000L
            if (database.sessionDao().consumeTimer(sessionId, deadline) != 1) return@withTransaction
            scheduler.cancelAlarm()
            val visible = appVisible && visibleSessionId == sessionId
            if (timely && settings.restTimerEnabled && (settings.restAlertsEnabled || (!fromAlarm && visible)))
                scheduler.feedback(settings.vibrationEnabled, visible)
        }
    }
    fun dismiss() = scheduler.dismiss()
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RestAlarmScheduler.ACTION_REST) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AreteApplication
                app.restTimers.elapsed(intent.getStringExtra("sessionId").orEmpty(), intent.getLongExtra("deadline", 0), true)
            } finally { result.finish() }
        }
    }
}

class RestRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { (context.applicationContext as AreteApplication).restTimers.reconcile(dropExpired = true) }
            finally { result.finish() }
        }
    }
}
