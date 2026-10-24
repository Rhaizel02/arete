package com.arete.app.data

import android.app.NotificationManager
import android.os.Bundle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arete.app.AreteApplication
import com.arete.app.data.local.*
import com.arete.app.reminders.RestAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingEngineDeviceTest {
    @Test fun roomMatchingAndDerivedVolumeUseActualAndroidSqlite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).build()
        try {
            val repository = AreteRepository(db, CatalogSeeder(context, db)); repository.initialize()
            var clock = System.currentTimeMillis() - 120_000
            val sessions = SessionRepository(db, now = { clock })
            val old = sessions.startFreeWorkout(); val oldExercise = sessions.addExercise(old, BENCH)
            val oldSets = db.sessionDao().getSets(oldExercise)
            sessions.completeSet(oldSets[0].copy(weightKg = 20.0, reps = 5, isWarmupSnapshot = true), false)
            sessions.completeSet(oldSets[1].copy(weightKg = 80.0, reps = 10), false)
            sessions.updateSet(oldSets[2].copy(weightKg = 999.0, reps = 9))
            clock += 60_000; sessions.finishSession(db.sessionDao().getSession(old)!!.session)
            clock += 1000
            val next = sessions.startFreeWorkout(); val nextExercise = sessions.addExercise(next, BENCH)
            assertTrue(db.sessionDao().getSets(nextExercise).all { it.weightKg == 80.0 && it.reps == 10 && !it.isWarmupSnapshot })
            val stats = ProgressRepository(db).dashboard(flowOf(3)).first()
            assertEquals(800.0, stats.externalVolume.totals.externalKgReps!!, 0.0)
            assertEquals(1, stats.externalVolume.totals.effectiveSets)
            assertEquals(next, sessions.startFreeWorkout())
        } finally { db.close() }
    }
    private companion object { const val BENCH = "arete-barbell-bench-press" }
}

/** Explicitly opt-in, disposable AVD only. No production fixture, broadcast control or network dependency. */
@RunWith(AndroidJUnit4::class)
class TrainingFixtureTest {
    @Test fun controlDisposableTrainingFixture() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("trainingFixture") ?: return@runBlocking
        val app = instrumentation.targetContext.applicationContext as AreteApplication
        val dao = app.database.sessionDao()
        val now = System.currentTimeMillis()
        if (mode == "seed") {
            app.automaticBackups.setEnabled(false)
            app.restTimers.restoring { app.database.backupDao().replaceAll(BackupDatabaseData()) }
            app.repository.initialize()
            app.preferences.setThemeMode(ThemeMode.DARK)
            app.preferences.setRestAlertsEnabled(true); app.preferences.setRestTimerEnabled(true); app.preferences.setVibrationEnabled(false)
            val r = "qa-training-routine"; val w = "qa-training-workout"
            app.database.routineDao().insertRoutine(RoutineEntity(r, "QA · Entrenamiento 2.0", true, now, now))
            app.database.routineDao().insertWorkout(WorkoutEntity(w, r, "Torso QA", 0, createdAt = now, updatedAt = now))
            listOf("arete-barbell-bench-press", "arete-pull-up").forEachIndexed { index, id ->
                val itemId = "qa-prescription-$index"
                app.database.routineDao().insertRoutineExercise(RoutineExerciseEntity(itemId, w, id, index, restSeconds = 30, createdAt = now, updatedAt = now))
                app.database.routineDao().insertSets((0..3).map { PrescriptionSetEntity("$itemId-$it", itemId, it,
                    if(it == 0) SetType.WARMUP else SetType.WORK, 8, 10) })
            }
            repeat(2) { index ->
                val end = now - (14 - index * 7) * 86_400_000L
                val id = "qa-training-history-$index"
                dao.insertSession(WorkoutSessionEntity(id, r, w, "QA · Entrenamiento 2.0", "Torso QA", end - 1800_000, end, SessionStatus.COMPLETED))
                listOf("arete-barbell-bench-press", "arete-pull-up").forEachIndexed { ex, exerciseId ->
                    val catalog = app.repository.getExercise(exerciseId)!!
                    val instance = "$id-$ex"
                    dao.insertExercises(listOf(SessionExerciseEntity(instance, id, exerciseId, ex, catalog.nameEs, catalog.nameEn, 30, null, "", null, "qa-prescription-$ex")))
                    dao.insertSets((0..3).map { set -> SessionSetEntity("$instance-$set", instance, set, 8, 10, set == 0,
                        weightKg = if (ex == 1) null else if(set == 0) 20.0 else 60.0 + index * 10 + (set - 1) * 2.5,
                        reps = if (set == 0) 5 else 10 - set, rir = 2, isCompleted = set < 3, completedAt = if(set < 3) end else null) })
                }
            }
        }
        if (mode == "light") app.preferences.setThemeMode(ThemeMode.LIGHT)
        if (mode == "dark") app.preferences.setThemeMode(ThemeMode.DARK)
        if (mode == "arm") {
            val active = dao.getActiveSession()!!.session
            val seconds = args.getString("seconds")?.toIntOrNull() ?: 10
            dao.startTimer(active.id, now + seconds * 1000L, seconds)
            app.restTimers.reconcile()
        }
        if (mode == "reconcile") app.restTimers.reconcile(dropExpired = true)
        if (mode == "skip") { dao.getActiveSession()?.session?.let { app.sessionRepository.skipTimer(it) }; app.restTimers.dismiss(); app.restTimers.reconcile() }
        if (mode == "enable") app.preferences.setRestAlertsEnabled(true)
        if (mode == "disable") { app.preferences.setRestAlertsEnabled(false); app.restTimers.reconcile() }
        if (mode == "expired") assertNull(dao.getActiveSession()!!.session.restTimerEndAt)
        if (mode == "notification") assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == RestAlarmScheduler.NOTIFICATION_ID })
        if (mode == "no-notification") assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id == RestAlarmScheduler.NOTIFICATION_ID })
        if (mode == "check-plan") {
            val template = app.database.routineDao().getWorkout("qa-training-workout")!!.orderedExercises().first()
            assertEquals(listOf(SetType.WORK, SetType.WORK, SetType.WORK, SetType.WARMUP, SetType.WORK), template.orderedSets().map { it.type })
            assertEquals("QA draft preserved", template.item.notes)
            assertEquals(20, template.item.restSeconds)
            assertEquals(6, template.orderedSets().first().repsMin)
            assertEquals(8, template.orderedSets().first().repsMax)
        }
        if (mode == "check-prefill") {
            val first = dao.getActiveSession()!!.orderedExercises().first()
            assertEquals(listOf(70.0, 72.5, 72.5, 20.0, 72.5), first.orderedSets().map { it.weightKg })
            assertEquals(listOf(9, 8, 8, 5, 8), first.orderedSets().map { it.reps })
            assertEquals(6, first.orderedSets().first().targetRepsMinSnapshot)
            assertEquals(8, first.orderedSets().first().targetRepsMaxSnapshot)
        }
        if (mode == "check-edited") {
            val first = dao.getActiveSession()!!.orderedExercises().first().orderedSets().first()
            assertEquals(81.5, first.weightKg!!, 0.0); assertEquals(10, first.reps); assertEquals(1, first.rir)
        }
        val active = dao.getActiveSession()
        val stats = app.progressRepository.dashboard(flowOf(3)).first()
        instrumentation.sendStatus(0, Bundle().apply {
            putString("fixture", "mode=$mode active=${active?.session?.id} deadline=${active?.session?.restTimerEndAt} sets=${active?.exercises?.sumOf { it.sets.size }} volume=${stats.externalVolume.totals.externalKgReps}")
        })
    }
}
