package com.arete.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupWorkTest {
    private lateinit var context: Context
    private val held = mutableListOf<CallbackToFutureAdapter.Completer<ListenableWorker.Result>>()
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    object : ListenableWorker(appContext, workerParameters) {
                        override fun startWork(): ListenableFuture<Result> = CallbackToFutureAdapter.getFuture { completer ->
                            held += completer
                            "Held worker for uniqueness verification"
                        }
                    }
            }).build())
        runBlocking { (context as com.arete.app.AreteApplication).automaticBackups.reconcile() }
    }

    @After fun finishHeldWorkers() {
        // Resolve before Robolectric closes Room; otherwise a future's finalizer could resume
        // a worker in the next test against an already-closed WorkManager database.
        held.toList().forEach { it.set(ListenableWorker.Result.failure()) }
        WorkManager.getInstance(context).cancelAllWork().result.get()
        held.clear()
    }

    @Test fun uniqueDailySurvivesRepeatedEnableAndCancelLeavesNoScheduledWork() = runBlocking {
        val scheduler = BackupWorkScheduler(context)
        val manager = WorkManager.getInstance(context)
        scheduler.daily(true)
        val first = manager.getWorkInfosForUniqueWork(BackupWorkScheduler.DAILY).get().single()
        repeat(3) { scheduler.daily(true) }
        val work = manager.getWorkInfosForUniqueWork(BackupWorkScheduler.DAILY).get().filter { !it.state.isFinished }
        assertEquals(1, work.size)
        assertEquals(first.id, work.single().id)
        // Run a repeat under the official test driver rather than waiting 24 hours.
        WorkManagerTestInitHelper.getTestDriver(context)!!.setPeriodDelayMet(first.id)
        scheduler.daily(false)
        assertTrue(manager.getWorkInfosForUniqueWork(BackupWorkScheduler.DAILY).get().all { it.state.isFinished })
    }

    @Test fun immediateRequestsAreUniqueWhilePending() = runBlocking {
        val scheduler = BackupWorkScheduler(context)
        val manager = WorkManager.getInstance(context)
        scheduler.requestNow()
        val first = manager.getWorkInfosForUniqueWork(BackupWorkScheduler.NOW).get().single().id
        repeat(3) { scheduler.requestNow() }
        val active = manager.getWorkInfosForUniqueWork(BackupWorkScheduler.NOW).get().filter { !it.state.isFinished }
        assertEquals(1, active.size)
        assertEquals(first, active.single().id)
    }
}
