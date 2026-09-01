package com.arete.app.data

import android.content.Context
import androidx.room.withTransaction
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val CatalogJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class CatalogExercise(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val instructions: String = "",
    val source: String? = null,
    val sourceId: String? = null,
    val externalUrl: String? = null,
    val imageUrl: String? = null
)

class CatalogSeeder(
    private val context: Context,
    private val database: AreteDatabase
) {
    suspend fun seedIfEmpty() {
        database.withTransaction {
            if (database.exerciseDao().count() != 0) return@withTransaction
            val text = context.assets.open("exercises_catalog.json").bufferedReader().use { it.readText() }
            val catalog = CatalogJson.decodeFromString<List<CatalogExercise>>(text)
            val timestamp = System.currentTimeMillis()
            database.exerciseDao().upsertAll(catalog.map { item ->
                ExerciseEntity(
                    id = item.id,
                    nameEs = item.nameEs,
                    nameEn = item.nameEn,
                    primaryMuscle = item.primaryMuscle,
                    secondaryMuscles = item.secondaryMuscles,
                    equipment = item.equipment,
                    instructions = item.instructions,
                    imageUrl = item.imageUrl,
                    externalUrl = item.externalUrl,
                    source = item.source,
                    sourceId = item.sourceId,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            })
        }
    }
}
