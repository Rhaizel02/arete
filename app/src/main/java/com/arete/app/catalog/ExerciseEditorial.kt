package com.arete.app.catalog

import com.arete.app.data.local.ExerciseEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.text.Normalizer
import java.util.Locale

/** Presentation-only metadata. Never written into Room, routines, snapshots or personal backups. */
@Serializable
data class EditorialManifest(
    val version: Int,
    val reviewedAt: String,
    val sources: List<EditorialSource>,
    val exercises: List<EditorialExercise>
) {
    fun forExercise(exercise: ExerciseEntity?): EditorialExercise? =
        exercise?.takeUnless { it.isCustom }?.let { current -> exercises.firstOrNull { it.exerciseId == current.id } }

    companion object {
        fun decode(raw: String): EditorialManifest = Json.decodeFromString<EditorialManifest>(raw).also { manifest ->
            require(manifest.version == 1)
            require(manifest.sources.map { it.id }.distinct().size == manifest.sources.size)
            require(manifest.exercises.map { it.exerciseId }.distinct().size == manifest.exercises.size)
            require(manifest.sources.all { it.id.isNotBlank() && it.title.isNotBlank() && isWebLink(it.url) })
            val sourceIds = manifest.sources.map { it.id }.toSet()
            manifest.exercises.forEach { entry ->
                require(entry.exerciseId.isNotBlank() && entry.sourceIds.isNotEmpty() && entry.sourceIds.all { it in sourceIds })
                require("es" in entry.text)
                entry.text.values.forEach { text ->
                    require(listOf(text.setup, text.steps, text.cues, text.mistakes, text.muscles).all { values -> values.isNotEmpty() && values.all(String::isNotBlank) })
                }
                entry.illustrations.forEach { art ->
                    require(art.resource.matches(Regex("[a-z][a-z0-9_]*")))
                    require(art.alt["es"].orEmpty().isNotBlank() && art.caption["es"].orEmpty().isNotBlank())
                    require(art.author.isNotBlank() && art.license.isNotBlank() && art.changes.isNotBlank())
                    require(isWebLink(art.sourceUrl) && isWebLink(art.licenseUrl))
                }
            }
        }
    }
}

@Serializable
data class EditorialSource(val id: String, val title: String, val url: String, val kind: String)

@Serializable
data class EditorialExercise(
    val exerciseId: String,
    val text: Map<String, TechnicalGuide>,
    val sourceIds: List<String>,
    val illustrations: List<ExerciseIllustration> = emptyList()
) {
    fun localized(language: String): Pair<String, TechnicalGuide> {
        val requested = language.lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        val actual = if (requested in text) requested else "es"
        return actual to text.getValue(actual)
    }
}

@Serializable
data class TechnicalGuide(
    val setup: List<String>, val steps: List<String>, val cues: List<String>,
    val mistakes: List<String>, val muscles: List<String>
)

@Serializable
data class ExerciseIllustration(
    val resource: String,
    val alt: Map<String, String>,
    val caption: Map<String, String>,
    val sourceUrl: String,
    val author: String,
    val license: String,
    val licenseUrl: String,
    val changes: String
) {
    fun altText(language: String) = alt[language] ?: alt.getValue("es")
    fun captionText(language: String) = caption[language] ?: caption.getValue("es")
}

fun isWebLink(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme?.lowercase(Locale.ROOT) in listOf("https", "http") && !uri.host.isNullOrBlank()
}.getOrDefault(false)

fun catalogSearchKey(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

fun filterExercises(exercises: List<ExerciseEntity>, query: String, muscle: String?, equipment: String?): List<ExerciseEntity> {
    val search = catalogSearchKey(query)
    return exercises.filter { exercise ->
        !exercise.isArchived &&
            (search.isEmpty() || catalogSearchKey(exercise.nameEs).contains(search) || catalogSearchKey(exercise.nameEn).contains(search)) &&
            (muscle == null || catalogSearchKey(exercise.primaryMuscle) == catalogSearchKey(muscle)) &&
            (equipment == null || catalogSearchKey(exercise.equipment) == catalogSearchKey(equipment))
    }
}

fun catalogFilterOptions(values: List<String>): List<String> = values.filter(String::isNotBlank)
    .distinctBy(::catalogSearchKey).sortedBy(::catalogSearchKey)
