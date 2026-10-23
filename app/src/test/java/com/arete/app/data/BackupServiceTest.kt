package com.arete.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.BackupDatabaseData
import com.arete.app.data.local.BodyWeightEntryEntity
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.SessionExerciseEntity
import com.arete.app.data.local.SessionSetEntity
import com.arete.app.data.local.SessionStatus
import com.arete.app.data.local.SetType
import com.arete.app.data.local.SkippedPlannedWorkoutEntity
import com.arete.app.data.local.WorkoutEntity
import com.arete.app.data.local.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupServiceTest {
    private lateinit var database: AreteDatabase
    private lateinit var preferences: PreferencesRepository
    private lateinit var service: BackupService
    private val clock = 1_800_000_000_000L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        preferences = PreferencesRepository(context)
        preferences.restore(BackupPreferences())
        service = BackupService(database, preferences, now = { clock })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun completeBackupRoundTripRestoresIdsRelationshipsSnapshotsPlanningAndPreferences() = runTest {
        seedEverything()
        val raw = service.export()
        val contract = Json.decodeFromString<AreteBackup>(raw)
        assertEquals(BACKUP_SCHEMA_VERSION, contract.backupSchemaVersion)
        assertEquals("session", contract.database.workoutSessions.single().id)
        assertEquals("Press banca histórico", contract.database.sessionExercises.single().exerciseNameEsSnapshot)

        database.backupDao().replaceAll(BackupDatabaseData())
        preferences.restore(BackupPreferences())
        assertTrue(database.backupDao().routines().isEmpty())

        service.restore(raw)
        val dao = database.backupDao()
        assertEquals(listOf("exercise"), dao.exercises().map { it.id })
        assertEquals(listOf("routine"), dao.routines().map { it.id })
        assertEquals("routine", dao.workouts().single().routineId)
        assertEquals("workout", dao.routineExercises().single().workoutId)
        assertEquals("routine-exercise", dao.prescriptionSets().single().routineExerciseId)
        assertEquals("session", dao.sessionExercises().single().sessionId)
        assertEquals("session-exercise", dao.sessionSets().single().sessionExerciseId)
        assertEquals(82.5, dao.sessionSets().single().weightKg!!, 0.0)
        assertEquals(78.4, dao.bodyWeights().single().weightKg, 0.0)
        assertEquals("workout", dao.skippedDays().single().workoutId)

        val settings = preferences.settings.first()
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertFalse(settings.restTimerEnabled)
        assertFalse(settings.vibrationEnabled)
        assertEquals(4, settings.weeklyGoal)
        assertTrue(settings.remindersEnabled)
        assertEquals(7, settings.reminderHour)
        assertEquals(15, settings.reminderMinute)
        assertEquals(WeeklyPlanEntry(Calendar.MONDAY, "workout"), settings.weeklyPlan.single())
    }

    @Test
    fun unsupportedOrRelationallyInvalidBackupNeverChangesExistingData() = runTest {
        seedEverything()
        val raw = service.export()
        assertRestoreFails(raw.replace("\"backupSchemaVersion\": 1", "\"backupSchemaVersion\": 99"))
        assertEquals("routine", database.backupDao().routines().single().id)

        assertRestoreFails(raw.replace("\"routineId\": \"routine\"", "\"routineId\": \"missing\""))
        assertEquals("workout", database.backupDao().workouts().single().id)
        assertEquals("session", database.backupDao().workoutSessions().single().id)
        assertRestoreFails(raw.replace("\"bodyWeights\":", "\"missingBodyWeights\":"))
        assertEquals("weight", database.backupDao().bodyWeights().single().id)
    }

    @Test
    fun databaseFailureRollsBackDeletedAndInsertedRowsAndLeavesPreferencesUntouched() = runTest {
        seedEverything()
        val raw = service.export()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON body_weight_entries BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertRestoreFails(raw)
        assertEquals("session", database.backupDao().workoutSessions().single().id)
        assertEquals("session-set", database.backupDao().sessionSets().single().id)
        assertEquals("weight", database.backupDao().bodyWeights().single().id)
        assertEquals(ThemeMode.DARK, preferences.settings.first().themeMode)
    }

    @Test
    fun malformedBackupHasClearErrorAndRoutineJsonV1RemainsSeparate() = runTest {
        var message = ""
        try {
            service.restore("{ esto no es json }")
        } catch (error: BackupImportException) {
            message = error.message.orEmpty()
        }
        assertTrue(message.contains("Backup JSON"))
        assertEquals(1, ROUTINE_JSON_SCHEMA_VERSION)
        assertTrue(database.backupDao().routines().isEmpty())
    }

    @Test
    fun preferencesFailureRollsBackFitnessAndCompensatesPreviousSettings() = runTest {
        seedEverything()
        val raw = service.export().replace("\"weeklyGoal\": 4", "\"weeklyGoal\": 6")
        var attempts = 0
        service = BackupService(database, preferences, now = { clock }, restorePreferences = { snapshot ->
            preferences.restore(snapshot)
            if (attempts++ == 0) throw java.io.IOException("Injected preferences failure after write")
        })
        assertRestoreFails(raw.replace("82.5", "90.0"))
        assertEquals(82.5, database.backupDao().sessionSets().single().weightKg!!, 0.0)
        assertEquals(4, preferences.settings.first().weeklyGoal)
        assertEquals(2, attempts)
    }

    @Test
    fun legacyV1FixtureRestoresWithoutAutomaticConfiguration() = runTest {
        val raw = javaClass.getResource("/backup-v1-legacy.json")!!.readText()
        service.restore(raw)
        assertEquals("legacy-routine", database.backupDao().routines().single().id)
        assertEquals("legacy-set", database.backupDao().sessionSets().single().id)
        assertEquals(75.0, database.backupDao().sessionSets().single().weightKg!!, 0.0)
        assertEquals(79.0, database.backupDao().bodyWeights().single().weightKg, 0.0)
        assertEquals(ThemeMode.DARK, preferences.settings.first().themeMode)
        assertEquals(1, Json.decodeFromString<AreteBackup>(service.export()).backupSchemaVersion)
    }

    private suspend fun seedEverything() {
        val dao = database.backupDao()
        dao.insertExercises(listOf(ExerciseEntity(
            id = "exercise",
            nameEs = "Press banca",
            nameEn = "Bench Press",
            primaryMuscle = "Pecho",
            equipment = "Barra",
            source = "test",
            sourceId = "bench",
            createdAt = 1,
            updatedAt = 2
        )))
        dao.insertRoutines(listOf(RoutineEntity("routine", "Torso", true, 1, 2)))
        dao.insertWorkouts(listOf(WorkoutEntity("workout", "routine", "Torso A", 0, "nota", 1, 2)))
        dao.insertRoutineExercises(listOf(RoutineExerciseEntity(
            "routine-exercise", "workout", "exercise", 0, 180, 2.5, "pausa", "A1", 1, 2
        )))
        dao.insertPrescriptionSets(listOf(PrescriptionSetEntity("prescription", "routine-exercise", 0, SetType.WORK, 8, 12)))
        dao.insertWorkoutSessions(listOf(WorkoutSessionEntity(
            "session", "routine", "workout", "Torso", "Torso A", 10, 20, SessionStatus.COMPLETED, "bien"
        )))
        dao.insertSessionExercises(listOf(SessionExerciseEntity(
            "session-exercise", "session", "exercise", 0, "Press banca histórico", "Historical Bench", 180, 2.5,
            "snapshot", "A1", "routine-exercise", "sesión"
        )))
        dao.insertSessionSets(listOf(SessionSetEntity(
            "session-set", "session-exercise", 0, 8, 12, false, 82.5, 10, 2, false, true, 19
        )))
        dao.insertBodyWeights(listOf(BodyWeightEntryEntity("weight", 78.4, clock - 1_000, "mañana")))
        dao.insertSkippedDays(listOf(SkippedPlannedWorkoutEntity(
            "skip", "routine", "workout", "Torso", "Torso A", 20_000, clock, "viaje"
        )))
        preferences.restore(BackupPreferences(
            themeMode = ThemeMode.DARK.name,
            restTimerEnabled = false,
            vibrationEnabled = false,
            weeklyGoal = 4,
            remindersEnabled = true,
            reminderHour = 7,
            reminderMinute = 15,
            weeklyPlan = listOf(WeeklyPlanEntry(Calendar.MONDAY, "workout"))
        ))
    }

    private suspend fun assertRestoreFails(raw: String) {
        var failed = false
        try {
            service.restore(raw)
        } catch (_: BackupImportException) {
            failed = true
        }
        assertTrue(failed)
    }
}
