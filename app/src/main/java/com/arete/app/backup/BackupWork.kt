package com.arete.app.backup

import android.content.Context
import androidx.work.*
import com.arete.app.AreteApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

interface BackupWorkScheduling {
    suspend fun daily(enabled: Boolean)
    suspend fun requestNow()
}

class BackupWorkScheduler(private val context: Context) : BackupWorkScheduling {
    private val manager get() = WorkManager.getInstance(context)
    val immediatePending get() = manager.getWorkInfosForUniqueWorkFlow(NOW).map { work -> work.any { !it.state.isFinished } }
    val dailyScheduled get() = manager.getWorkInfosForUniqueWorkFlow(DAILY).map { work -> work.any { !it.state.isFinished } }

    override suspend fun daily(enabled: Boolean) = withContext(Dispatchers.IO) {
        val operation = if (enabled) manager.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .setInputData(workDataOf(AUTOMATIC to true)).build())
        else manager.cancelUniqueWork(DAILY)
        operation.result.get()
        Unit
    }

    override suspend fun requestNow() = withContext(Dispatchers.IO) {
        manager.enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BackupWorker>().setInputData(workDataOf(AUTOMATIC to false)).build()).result.get()
        Unit
    }

    companion object {
        const val DAILY = "arete.backup.daily"
        const val NOW = "arete.backup.now"
        const val AUTOMATIC = "automatic"
    }
}

class BackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext.applicationContext as AreteApplication
        return if (app.automaticBackups.createCopy(inputData.getBoolean(BackupWorkScheduler.AUTOMATIC, false)))
            Result.success() else Result.failure()
    }
}
