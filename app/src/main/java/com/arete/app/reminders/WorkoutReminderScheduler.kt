package com.arete.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arete.app.AreteApplication
import com.arete.app.MainActivity
import com.arete.app.R
import com.arete.app.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class WorkoutReminderScheduler(
    private val context: Context,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(settings: AppSettings) {
        cancel()
        val triggerAt = nextTriggerAt(settings, now(), timeZone()) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
    }

    fun cancel() = alarmManager.cancel(pendingIntent())

    internal fun nextTriggerAt(settings: AppSettings, currentTime: Long, zone: TimeZone): Long? {
        if (!settings.remindersEnabled || settings.weeklyPlan.isEmpty()) return null
        val plannedDays = settings.weeklyPlan.mapTo(hashSetOf()) { it.dayOfWeek }
        for (offset in 0..7) {
            val candidate = Calendar.getInstance(zone).apply {
                timeInMillis = currentTime
                add(Calendar.DAY_OF_MONTH, offset)
                set(Calendar.HOUR_OF_DAY, settings.reminderHour)
                set(Calendar.MINUTE, settings.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in plannedDays && candidate.timeInMillis > currentTime) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    fun showNotification(workoutName: String, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        val channelId = if (vibrationEnabled) CHANNEL_VIBRATION else CHANNEL_SILENT
        createChannels()
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Entrenamiento planificado")
            .setContentText("Hoy toca $workoutName")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(if (vibrationEnabled) android.app.Notification.DEFAULT_VIBRATE else 0)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_VIBRATION, "Recordatorios de entrenamiento", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos en los días de entrenamiento planificados"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT, "Recordatorios sin vibración", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos de entrenamiento sin vibración"
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        41,
        Intent(context, WorkoutReminderReceiver::class.java).setAction(ACTION_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val ACTION_REMINDER = "com.arete.app.TRAINING_REMINDER"
        private const val CHANNEL_VIBRATION = "training_reminders_vibration"
        private const val CHANNEL_SILENT = "training_reminders_silent"
        private const val NOTIFICATION_ID = 410
    }
}

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AreteApplication
                if (intent.action == WorkoutReminderScheduler.ACTION_REMINDER && app.planningRepository.shouldSendReminderNow()) {
                    val nextWorkout = app.sessionRepository.homeState.first().nextWorkout?.workoutName ?: "tu próximo entrenamiento"
                    val settings = app.preferences.settings.first()
                    app.reminderScheduler.showNotification(nextWorkout, settings.vibrationEnabled)
                }
                app.scheduleCurrentReminder()
            } finally {
                result.finish()
            }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                (context.applicationContext as AreteApplication).scheduleCurrentReminder()
            } finally {
                result.finish()
            }
        }
    }
}
