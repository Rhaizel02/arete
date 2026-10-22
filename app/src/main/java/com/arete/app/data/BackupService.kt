package com.arete.app.data

import androidx.room.withTransaction
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.BackupDatabaseData
import com.arete.app.data.local.SessionStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class AreteBackup(
    val backupSchemaVersion: Int,
    val exportedAt: Long,
    val database: BackupDatabaseData,
    val preferences: BackupPreferences
)

class BackupImportException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class BackupService(
    private val database: AreteDatabase,
    private val preferences: PreferencesRepository,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    suspend fun export(): String {
        val data = database.withTransaction {
            val dao = database.backupDao()
            BackupDatabaseData(
                exercises = dao.exercises(),
                routines = dao.routines(),
                workouts = dao.workouts(),
                routineExercises = dao.routineExercises(),
                prescriptionSets = dao.prescriptionSets(),
                workoutSessions = dao.workoutSessions(),
                sessionExercises = dao.sessionExercises(),
                sessionSets = dao.sessionSets(),
                bodyWeights = dao.bodyWeights(),
                skippedDays = dao.skippedDays()
            )
        }
        return json.encodeToString(
            AreteBackup(BACKUP_SCHEMA_VERSION, now(), data, preferences.backupSnapshot())
        )
    }

    /**
     * Full replacement semantics. JSON and relationships are validated before the first write;
     * all Room tables are then replaced inside one transaction. DataStore follows as a single edit.
     */
    suspend fun restore(raw: String) {
        val backup = decodeAndValidate(raw)
        try {
            database.withTransaction { database.backupDao().replaceAll(backup.database) }
        } catch (error: Exception) {
            throw BackupImportException("No se pudo restaurar el backup; los datos locales no se han modificado", error)
        }
        preferences.restore(backup.preferences)
    }

    internal fun decodeAndValidate(raw: String): AreteBackup {
        val backup = try {
            val root = json.parseToJsonElement(raw).jsonObject
            val tables = root["database"]?.jsonObject ?: invalid("Faltan los datos del backup")
            val required = setOf("exercises", "routines", "workouts", "routineExercises", "prescriptionSets", "workoutSessions", "sessionExercises", "sessionSets", "bodyWeights", "skippedDays")
            if (!tables.keys.containsAll(required)) invalid("El backup está incompleto; faltan tablas")
            json.decodeFromString<AreteBackup>(raw)
        } catch (error: SerializationException) {
            throw BackupImportException("El archivo no es un Backup JSON válido", error)
        } catch (error: IllegalArgumentException) {
            throw BackupImportException("El archivo no es un Backup JSON válido", error)
        }
        if (backup.backupSchemaVersion != BACKUP_SCHEMA_VERSION) {
            throw BackupImportException(
                "backupSchemaVersion ${backup.backupSchemaVersion} no está soportada; se esperaba $BACKUP_SCHEMA_VERSION"
            )
        }
        validateDatabase(backup.database)
        validatePreferences(backup.preferences)
        return backup
    }

    private fun validateDatabase(data: BackupDatabaseData) {
        requireUnique("ejercicios", data.exercises.map { it.id })
        requireUnique("rutinas", data.routines.map { it.id })
        requireUnique("workouts", data.workouts.map { it.id })
        requireUnique("ejercicios de rutina", data.routineExercises.map { it.id })
        requireUnique("prescripciones", data.prescriptionSets.map { it.id })
        requireUnique("sesiones", data.workoutSessions.map { it.id })
        requireUnique("ejercicios de sesión", data.sessionExercises.map { it.id })
        requireUnique("series de sesión", data.sessionSets.map { it.id })
        requireUnique("pesos", data.bodyWeights.map { it.id })
        requireUnique("días omitidos", data.skippedDays.map { it.id })
        requireUnique("fechas de peso", data.bodyWeights.map { it.recordedAt })

        val exerciseIds = data.exercises.mapTo(hashSetOf()) { it.id }
        val routineIds = data.routines.mapTo(hashSetOf()) { it.id }
        val workoutIds = data.workouts.mapTo(hashSetOf()) { it.id }
        val routineExerciseIds = data.routineExercises.mapTo(hashSetOf()) { it.id }
        val sessionIds = data.workoutSessions.mapTo(hashSetOf()) { it.id }
        val sessionExerciseIds = data.sessionExercises.mapTo(hashSetOf()) { it.id }

        if (data.routines.count { it.isActive } > 1) invalid("Solo puede haber una rutina activa")
        if (data.workoutSessions.count { it.status == SessionStatus.IN_PROGRESS } > 1) invalid("Solo puede haber una sesión en curso")
        requireUnique("referencias de catálogo", data.exercises.filter { it.source != null && it.sourceId != null }.map { it.source to it.sourceId })
        data.workouts.forEach {
            if (it.routineId !in routineIds) invalid("El workout ${it.id} referencia una rutina inexistente")
            if (it.name.isBlank()) invalid("El workout ${it.id} no tiene nombre")
        }
        data.routineExercises.forEach {
            if (it.workoutId !in workoutIds || it.exerciseId !in exerciseIds) invalid("Relación de ejercicio de rutina inválida: ${it.id}")
            if (it.restSeconds !in 0..3_600 || (it.incrementKg != null && (!it.incrementKg.isFinite() || it.incrementKg < 0.0))) {
                invalid("Configuración de ejercicio de rutina inválida: ${it.id}")
            }
        }
        data.prescriptionSets.forEach {
            if (it.routineExerciseId !in routineExerciseIds || it.repsMin <= 0 || it.repsMax < it.repsMin) {
                invalid("Prescripción inválida: ${it.id}")
            }
        }
        data.workoutSessions.forEach {
            if (it.routineId != null && it.routineId !in routineIds) invalid("La sesión ${it.id} referencia una rutina inexistente")
            if (it.sourceWorkoutId != null && it.sourceWorkoutId !in workoutIds) invalid("La sesión ${it.id} referencia un workout inexistente")
            if (it.status == SessionStatus.COMPLETED && it.completedAt == null) invalid("La sesión completada ${it.id} no tiene fecha final")
        }
        data.sessionExercises.forEach {
            if (it.sessionId !in sessionIds || (it.exerciseId != null && it.exerciseId !in exerciseIds)) {
                invalid("Relación de ejercicio de sesión inválida: ${it.id}")
            }
        }
        data.sessionSets.forEach {
            if (it.sessionExerciseId !in sessionExerciseIds || it.targetRepsMinSnapshot <= 0 ||
                it.targetRepsMaxSnapshot < it.targetRepsMinSnapshot ||
                (it.weightKg != null && (!it.weightKg.isFinite() || it.weightKg < 0.0)) || (it.reps != null && it.reps < 0)
            ) invalid("Serie de sesión inválida: ${it.id}")
        }
        data.bodyWeights.forEach {
            if (!it.weightKg.isFinite() || it.weightKg !in 1.0..1_000.0) invalid("Registro de peso inválido: ${it.id}")
        }
        data.skippedDays.forEach {
            if (it.routineId != null && it.routineId !in routineIds) invalid("El día omitido ${it.id} referencia una rutina inexistente")
            if (it.workoutId != null && it.workoutId !in workoutIds) invalid("El día omitido ${it.id} referencia un workout inexistente")
        }
        requireUnique("posiciones de workouts", data.workouts.map { it.routineId to it.position })
        requireUnique("posiciones de rutina", data.routineExercises.map { it.workoutId to it.position })
        requireUnique("posiciones de prescripción", data.prescriptionSets.map { it.routineExerciseId to it.position })
        requireUnique("posiciones de ejercicio de sesión", data.sessionExercises.map { it.sessionId to it.position })
        requireUnique("posiciones de serie", data.sessionSets.map { it.sessionExerciseId to it.position })
        requireUnique("días omitidos", data.skippedDays.map { it.plannedEpochDay to it.workoutId })
    }

    private fun validatePreferences(value: BackupPreferences) {
        if (runCatching { ThemeMode.valueOf(value.themeMode) }.isFailure) invalid("Tema de backup no válido")
        if (value.weeklyGoal !in 1..14) invalid("Objetivo semanal no válido")
        if (value.reminderHour !in 0..23 || value.reminderMinute !in 0..59) invalid("Hora de recordatorio no válida")
        if (value.weeklyPlan.any { it.dayOfWeek !in 1..7 || it.workoutId.isBlank() } ||
            value.weeklyPlan.map { it.dayOfWeek }.distinct().size != value.weeklyPlan.size
        ) invalid("Planificación semanal no válida")
    }

    private fun <T> requireUnique(label: String, values: List<T>) {
        if (values.distinct().size != values.size) invalid("El backup contiene $label duplicados")
    }

    private fun invalid(message: String): Nothing = throw BackupImportException(message)
}
