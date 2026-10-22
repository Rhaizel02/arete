package com.arete.app.data

import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arete.app.reminders.WorkoutReminderScheduler
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderNotificationTest {
    @Test
    fun localReminderPostsAndOpensTheApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
            ).use { it.readBytes() }
        }
        val scheduler = WorkoutReminderScheduler(context)
        scheduler.showNotification("Torso A", vibrationEnabled = false)
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            val deadline = android.os.SystemClock.elapsedRealtime() + 3_000
            while (manager.activeNotifications.isEmpty() && android.os.SystemClock.elapsedRealtime() < deadline) {
                android.os.SystemClock.sleep(50)
            }
            assertTrue(manager.activeNotifications.any {
                it.notification.extras.getString("android.text") == "Hoy toca Torso A" && it.notification.contentIntent != null
            })
        } finally {
            manager.cancelAll()
        }
    }
}
