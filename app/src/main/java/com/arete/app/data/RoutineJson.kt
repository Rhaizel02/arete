package com.arete.app.data

import androidx.room.withTransaction
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.SetType
import com.arete.app.data.local.WorkoutEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.UUID

const val ROUTINE_JSON_SCHEMA_VERSION = 1

@Serializable
data class RoutineJson(
    val schemaVersion: Int,
    val name: String,
    val workouts: List<WorkoutJson>
)

@Serializable
data class WorkoutJson(
    val name: String,
    val notes: String? = null,
    val exercises: List<RoutineExerciseJson>
)

@Serializable
data class RoutineExerciseJson(
    val exercise: ExerciseRefJson,
    val restSeconds: Int = 120,
    val incrementKg: Double? = null,
    val notes: String? = null,
    val groupId: String? = null,
    val sets: List<PrescriptionSetJson>
)

@Serializable
data class ExerciseRefJson(
    val id: String? = null,
    val name: String? = null,
    val nameEn: String? = null,
    val primaryMuscle: String? = null,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String? = null,
    val instructions: String? = null
)

@Serializable
data class PrescriptionSetJson(
    val type: SetType = SetType.WORK,
    val reps: RepsJson
)

@Serializable
data class RepsJson(
    val exact: Int? = null,
    val min: Int? = null,
    val max: Int? = null
) {
    fun bounds(): Pair<Int, Int> = when {
        exact != null && min == null && max == null && exact > 0 -> exact to exact
        exact == null && min != null && max != null && min > 0 && min <= max -> min to max
        else -> throw RoutineImportException("Cada serie debe usar reps.exact o un rango min/max válido")
    }

    companion object {
        fun fromBounds(min: Int, max: Int): RepsJson =
            if (min == max) RepsJson(exact = min) else RepsJson(min = min, max = max)
    }
}

class RoutineImportException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class RoutineJsonService(
    private val database: AreteDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    suspend fun exportRoutine(routineId: String): String {
        val routine = database.routineDao().getRoutine(routineId)
            ?: throw IllegalArgumentException("La rutina no existe")
        val contract = RoutineJson(
            schemaVersion = ROUTINE_JSON_SCHEMA_VERSION,
            name = routine.routine.name,
            workouts = routine.orderedWorkouts().map { workout ->
                WorkoutJson(
                    name = workout.workout.name,
                    notes = workout.workout.notes.takeIf(String::isNotBlank),
                    exercises = workout.orderedExercises().map { item ->
                        RoutineExerciseJson(
                            exercise = ExerciseRefJson(
                                id = item.exercise.id,
                                name = item.exercise.nameEs,
                                nameEn = item.exercise.nameEn.takeIf(String::isNotBlank),
                                primaryMuscle = item.exercise.primaryMuscle,
                                secondaryMuscles = item.exercise.secondaryMuscles,
                                equipment = item.exercise.equipment,
                                instructions = item.exercise.instructions.takeIf(String::isNotBlank)
                            ),
                            restSeconds = item.item.restSeconds,
                            incrementKg = item.item.incrementKg,
                            notes = item.item.notes.takeIf(String::isNotBlank),
                            groupId = item.item.groupId,
                            sets = item.orderedSets().map { set ->
                                PrescriptionSetJson(set.type, RepsJson.fromBounds(set.repsMin, set.repsMax))
                            }
                        )
                    }
                )
            }
        )
        return json.encodeToString(contract)
    }

    suspend fun importRoutine(rawJson: String): String {
        val contract = try {
            json.decodeFromString<RoutineJson>(rawJson)
        } catch (error: SerializationException) {
            throw RoutineImportException("El archivo no es un Routine JSON válido", error)
        }
        validate(contract)

        return database.withTransaction {
            val timestamp = now()
            val routineId = newId()
            database.routineDao().insertRoutine(
                RoutineEntity(routineId, contract.name.trim(), false, timestamp, timestamp)
            )
            contract.workouts.forEachIndexed { workoutIndex, workoutJson ->
                val workoutId = newId()
                database.routineDao().insertWorkout(
                    WorkoutEntity(
                        id = workoutId,
                        routineId = routineId,
                        name = workoutJson.name.trim(),
                        position = workoutIndex,
                        notes = workoutJson.notes.orEmpty().trim(),
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
                workoutJson.exercises.forEachIndexed { exerciseIndex, itemJson ->
                    val exercise = resolveExercise(itemJson.exercise, timestamp)
                    val itemId = newId()
                    database.routineDao().insertRoutineExercise(
                        RoutineExerciseEntity(
                            id = itemId,
                            workoutId = workoutId,
                            exerciseId = exercise.id,
                            position = exerciseIndex,
                            restSeconds = itemJson.restSeconds,
                            incrementKg = itemJson.incrementKg,
                            notes = itemJson.notes.orEmpty().trim(),
                            groupId = itemJson.groupId?.trim()?.takeIf(String::isNotEmpty),
                            createdAt = timestamp,
                            updatedAt = timestamp
                        )
                    )
                    database.routineDao().insertSets(itemJson.sets.mapIndexed { setIndex, setJson ->
                        val (min, max) = setJson.reps.bounds()
                        PrescriptionSetEntity(newId(), itemId, setIndex, setJson.type, min, max)
                    })
                }
            }
            routineId
        }
    }

    private fun validate(contract: RoutineJson) {
        if (contract.schemaVersion != ROUTINE_JSON_SCHEMA_VERSION) {
            throw RoutineImportException(
                "schemaVersion ${contract.schemaVersion} no está soportada; se esperaba $ROUTINE_JSON_SCHEMA_VERSION"
            )
        }
        if (contract.name.isBlank()) throw RoutineImportException("La rutina necesita un nombre")
        contract.workouts.forEachIndexed { workoutIndex, workout ->
            if (workout.name.isBlank()) throw RoutineImportException("El workout ${workoutIndex + 1} necesita un nombre")
            workout.exercises.forEachIndexed { exerciseIndex, item ->
                val ref = item.exercise
                if (ref.id.isNullOrBlank() && ref.name.isNullOrBlank() && ref.nameEn.isNullOrBlank()) {
                    throw RoutineImportException("El ejercicio ${exerciseIndex + 1} de ${workout.name} no se puede identificar")
                }
                if (item.restSeconds !in 0..3600) {
                    throw RoutineImportException("El descanso de ${ref.name ?: ref.nameEn} no es válido")
                }
                if (item.incrementKg != null && (!item.incrementKg.isFinite() || item.incrementKg < 0.0)) {
                    throw RoutineImportException("El incremento de ${ref.name ?: ref.nameEn} no es válido")
                }
                if (item.sets.isEmpty()) throw RoutineImportException("${ref.name ?: ref.nameEn} necesita al menos una serie")
                item.sets.forEach { it.reps.bounds() }
            }
        }
    }

    private suspend fun resolveExercise(ref: ExerciseRefJson, timestamp: Long): ExerciseEntity {
        val dao = database.exerciseDao()
        ref.id?.takeIf(String::isNotBlank)?.let { dao.findById(it) }?.let { return it }
        ref.name?.trim()?.takeIf(String::isNotEmpty)?.let { dao.findByNameEs(it) }?.let { return it }
        ref.nameEn?.trim()?.takeIf(String::isNotEmpty)?.let { dao.findByNameEn(it) }?.let { return it }

        val id = ref.id?.trim()?.takeIf(String::isNotEmpty) ?: newId()
        val nameEs = ref.name?.trim()?.takeIf(String::isNotEmpty)
            ?: ref.nameEn?.trim()?.takeIf(String::isNotEmpty)
            ?: throw RoutineImportException("No se puede crear un ejercicio sin nombre")
        val exercise = ExerciseEntity(
            id = id,
            nameEs = nameEs,
            nameEn = ref.nameEn?.trim().orEmpty(),
            primaryMuscle = ref.primaryMuscle?.trim()?.takeIf(String::isNotEmpty) ?: "Otros",
            secondaryMuscles = ref.secondaryMuscles.map(String::trim).filter(String::isNotEmpty),
            equipment = ref.equipment?.trim()?.takeIf(String::isNotEmpty) ?: "Otro",
            instructions = ref.instructions?.trim().orEmpty(),
            isCustom = true,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        dao.upsert(exercise)
        return exercise
    }
}
