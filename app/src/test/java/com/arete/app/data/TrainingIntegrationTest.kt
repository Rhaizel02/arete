package com.arete.app.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TrainingIntegrationTest {
    @Test fun historicalEditsDeletionAndV1RestoreRecalculateVolumeWithoutChangingRecords() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        try {
            var clock = 1_800_000_000_000L
            val repository = AreteRepository(db, CatalogSeeder(context, db)); repository.initialize()
            val sessions = SessionRepository(db, now = { clock })
            val progress = ProgressRepository(db, now = { clock })
            val prefs = PreferencesRepository(context, MemoryPreferences())
            prefs.setRestAlertsEnabled(true)
            val backup = BackupService(db, prefs)
            val id = sessions.startFreeWorkout()
            val exercise = sessions.addExercise(id, "arete-barbell-bench-press")
            val first = db.sessionDao().getSets(exercise).first()
            sessions.completeSet(first.copy(weightKg = 80.0, reps = 10), false)
            assertNull(progress.dashboard(flowOf(3)).first().externalVolume.totals.externalKgReps) // Active excluded.
            clock += 60_000
            sessions.finishSession(db.sessionDao().getSession(id)!!.session)
            val exported = backup.export()
            assertFalse(exported.contains("restAlertsEnabled") || exported.contains("rest_alerts_enabled"))
            val before = progress.dashboard(flowOf(3)).first()
            assertEquals(800.0, before.externalVolume.totals.externalKgReps!!, 0.0)
            sessions.updateSet(db.sessionDao().getSet(first.id)!!.copy(isWarmupSnapshot = true))
            assertNull(progress.dashboard(flowOf(3)).first().externalVolume.totals.externalKgReps)
            sessions.deleteSession(db.sessionDao().getSession(id)!!.session)
            prefs.setRestAlertsEnabled(false)
            backup.restore(exported)
            assertFalse(prefs.settings.first().restAlertsEnabled) // Local opt-in is not imported from JSON.
            val after = progress.dashboard(flowOf(3)).first()
            assertEquals(before.externalVolume, after.externalVolume)
            assertEquals(before.exercises.single().bestE1RmKg, after.exercises.single().bestE1RmKg)
            assertEquals(before.latestRecord, after.latestRecord)
        } finally { db.close() }
    }

    @Test fun prescriptionOptimisticCheckRejectsConcurrentChangesInsideRoomTransaction() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = AreteRepository(db, CatalogSeeder(context, db)); repository.initialize()
            val r = repository.createRoutine("Test"); val w = repository.addWorkout(r, "A")
            repository.addExercise(w, "arete-barbell-bench-press")
            val original = db.routineDao().getWorkout(w)!!.orderedExercises().first()
            val drafts = listOf(SetDraft(SetType.WORK, 5, 8))
            repository.savePrescription(original.item, 90, null, "new", null, drafts, original.orderedSets())
            try {
                repository.savePrescription(original.item, 30, null, "stale", null, drafts, original.orderedSets())
                fail("Concurrent edit was overwritten")
            } catch (_: IllegalStateException) { }
            val current = db.routineDao().getWorkout(w)!!.orderedExercises().first()
            assertEquals("new", current.item.notes)
            assertEquals(90, current.item.restSeconds)
            assertEquals(1, current.sets.size)
        } finally { db.close() }
    }
}
