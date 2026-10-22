package com.arete.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arete.app.AreteApplication
import com.arete.app.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/** Opt-in screenshot fixture: never executed by the ordinary instrumentation suite. */
@RunWith(AndroidJUnit4::class)
class DesignFixtureTest {
    @Test fun seedScreenshotDevice() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("designFixture") != "true") return@runBlocking
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AreteApplication
        val now = System.currentTimeMillis()
        val names = listOf("Press de banca con barra", "Press inclinado con mancuernas", "Remo con barra (Pendlay)", "Sentadilla trasera")
        val exercises = names.mapIndexed { i, name -> ExerciseEntity("design-e$i", name, "", if (i < 2) "Pecho" else if (i == 2) "Espalda" else "Piernas", equipment = "Barra", instructions = "Mantén los pies apoyados, estabiliza el tronco y controla el descenso.\n\nRealiza el movimiento con un rango cómodo y una carga que puedas controlar.", externalUrl = "https://exrx.net/WeightExercises/PectoralSternal/BBBenchPress", createdAt = now, updatedAt = now) }
        val workout = WorkoutEntity("design-workout", "design-routine", "Torso A", 0, createdAt = now, updatedAt = now)
        val routineExercises = exercises.mapIndexed { i, e -> RoutineExerciseEntity("design-r$i", workout.id, e.id, i, 120, 2.5, "", if(i < 2) "A" else null, now, now) }
        val historyCount = (args.getString("historySessions")?.toIntOrNull() ?: 7).coerceIn(0, 7)
        val sessions = (0 until historyCount).map { i ->
            val end = now - (7 - i) * 3 * 86_400_000L
            WorkoutSessionEntity("design-s$i", "design-routine", workout.id, "Torso / Pierna", workout.name, end - 3_000_000, end, SessionStatus.COMPLETED)
        } + WorkoutSessionEntity("design-active", "design-routine", workout.id, "Torso / Pierna", workout.name, now - 20 * 60_000)
        val sessionExercises = sessions.flatMap { s -> exercises.mapIndexed { i, e -> SessionExerciseEntity("${s.id}-e$i", s.id, e.id, i, e.nameEs, "", 120, 2.5, "", if(i < 2) "A" else null) } }
        val sets = sessionExercises.flatMap { e -> (0..3).map { index ->
            val sessionIndex = sessions.indexOfFirst { it.id == e.sessionId }
            val active = e.sessionId == "design-active"
            SessionSetEntity("${e.id}-set$index", e.id, index, 8, 12, false,
                weightKg = if (active) 80.0 else 60.0 + sessionIndex * 2.5,
                reps = if(index == 3) 8 else 10, rir = 2,
                isCompleted = !active || (e.position == 0 && index == 0),
                completedAt = if(!active) sessions[sessionIndex].completedAt else if(e.position == 0 && index == 0) now else null)
        } }
        app.database.backupDao().replaceAll(BackupDatabaseData(
            exercises, listOf(RoutineEntity("design-routine", "Torso / Pierna", true, now, now)), listOf(workout), routineExercises,
            routineExercises.flatMap { e -> (0..3).map { PrescriptionSetEntity("${e.id}-$it", e.id, it, SetType.WORK, 8, 12) } },
            sessions, sessionExercises, sets,
            (0..6).map { BodyWeightEntryEntity("design-weight$it", 79.0 - it * .15, now - (6 - it) * 4 * 86_400_000L) }, emptyList()
        ))
        app.preferences.restore(BackupPreferences(themeMode = args.getString("theme") ?: "DARK", weeklyGoal = 3,
            weeklyPlan = (1..7).map { WeeklyPlanEntry(it, workout.id) }))
    }
}
