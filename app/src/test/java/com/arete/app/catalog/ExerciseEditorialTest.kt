package com.arete.app.catalog

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.AreteRepository
import com.arete.app.data.CatalogSeeder
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.ui.artworkResource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExerciseEditorialTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun raw() = context.assets.open("exercise_editorial.json").bufferedReader().use { it.readText() }
    private fun manifest() = EditorialManifest.decode(raw())
    private fun exercise(id: String = "arete-barbell-bench-press", name: String = "Press de banca", english: String = "Bench Press", muscle: String = "Pecho", equipment: String = "Barra", custom: Boolean = false) =
        ExerciseEntity(id, name, english, muscle, equipment = equipment, isCustom = custom, instructions = "Mi instrucción", createdAt = 1, updatedAt = 1)

    @Test fun manifestCoversExactlyTenStableIdsWithoutChangingThe45ExerciseCatalog() {
        val editorial = manifest()
        val catalog = context.assets.open("exercises_catalog.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonArray }
        val ids = catalog.map { it.jsonObject.getValue("id").jsonPrimitive.content }.toSet()
        assertEquals(45, ids.size)
        assertEquals(10, editorial.exercises.size)
        assertTrue(editorial.exercises.all { it.exerciseId in ids && it.text.keys == setOf("es", "en") })
        assertEquals(3, editorial.exercises.count { it.illustrations.isNotEmpty() })
        assertTrue(editorial.sources.any { it.kind == "general-evidence" && it.url.endsWith("MSS.0000000000003897") })
        assertTrue(editorial.exercises.all { entry -> entry.sourceIds.any { id -> editorial.sources.any { it.id == id && it.kind == "practical-technique" } } })
    }

    @Test fun combinedFiltersMatchBothLanguagesAccentsAndCustomValues() {
        val list = listOf(exercise(), exercise("custom", "Press casero", "Home Press", equipment = "Banda casera", custom = true),
            exercise("curl", "Curl de bíceps", "Biceps Curl", "Bíceps", "Mancuernas"))
        assertEquals(listOf("arete-barbell-bench-press"), filterExercises(list, "BENCH", "pecho", "barra").map { it.id })
        assertEquals(listOf("custom"), filterExercises(list, "press", "Pecho", "Banda casera").map { it.id })
        assertEquals(listOf("curl"), filterExercises(list, "biceps", "Biceps", "Mancuernas").map { it.id })
        assertTrue(filterExercises(list, "bench", "Bíceps", null).isEmpty())
        assertEquals(list, filterExercises(list, "  ", null, null))
        assertEquals(listOf("Banda casera", "Barra"), catalogFilterOptions(listOf("Barra", "barra", "", "Banda casera")))
    }

    @Test fun customMissingAndBasicExercisesKeepTheirOwnContent() {
        val editorial = manifest()
        val custom = exercise(custom = true)
        assertNull(editorial.forExercise(custom)) // Even a misleading ID must not replace a custom guide.
        assertEquals("Mi instrucción", custom.instructions)
        assertNull(editorial.forExercise(null))
        assertNull(editorial.forExercise(exercise("missing")))
        assertNull(editorial.forExercise(exercise("arete-push-up")))
        assertTrue(editorial.forExercise(exercise("arete-pull-up"))!!.illustrations.isEmpty())
    }

    @Test fun localizationHasExplicitSpanishFallbackAndAccessibleDescriptions() {
        val entry = manifest().exercises.first()
        assertEquals("en", entry.localized("en-GB").first)
        assertEquals("es", entry.localized("fr").first)
        assertEquals("es", entry.copy(text = entry.text.filterKeys { it == "es" }).localized("en").first)
        entry.illustrations.forEach { assertTrue(it.altText("es").isNotBlank()); assertTrue(it.altText("en").isNotBlank()) }
    }

    @Test fun artworkIsLocalDecodableSmallAndHasExplicitCredits() {
        val images = manifest().exercises.flatMap { it.illustrations }
        assertEquals(6, images.size)
        images.forEach { art ->
            val resource = artworkResource(art.resource)
            assertNotNull(resource)
            val bitmap = BitmapFactory.decodeResource(context.resources, resource!!)
            assertNotNull(bitmap)
            assertTrue(bitmap.width <= 512 && bitmap.height <= 512)
            bitmap.recycle()
            assertTrue(art.author.contains("Everkinetic"))
            assertEquals("CC BY-SA 3.0", art.license)
            assertTrue(isWebLink(art.sourceUrl) && isWebLink(art.licenseUrl))
        }
        assertNull(artworkResource("nonexistent"))
    }

    @Test fun manifestRejectsDuplicateIdsAndBrokenReferences() {
        assertThrows(IllegalArgumentException::class.java) { EditorialManifest.decode(raw().replace("arete-pull-up", "arete-back-squat")) }
        assertThrows(IllegalArgumentException::class.java) { EditorialManifest.decode(raw().replace("\"sourceIds\":[\"raise-ace\"", "\"sourceIds\":[\"unknown\"")) }
        assertThrows(IllegalArgumentException::class.java) { EditorialManifest.decode(raw().replace("\"resource\":\"bench_press_lowered\"", "\"resource\":\"https://example.com/image\"")) }
    }

    @Test fun linksRejectUnsupportedSchemesAndMissingHosts() {
        assertFalse(isWebLink("javascript:alert(1)"))
        assertFalse(isWebLink("file:///sdcard/image.png"))
        assertFalse(isWebLink("https:relative"))
        assertTrue(isWebLink("https://www.acefitness.org/"))
    }

    @Test fun repositoryReadResolvesArchivedGuideByIdWithoutWritingCustomOrHistoryData() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = AreteRepository(db, CatalogSeeder(context, db))
            repository.initialize()
            val custom = exercise("custom", custom = true)
            db.exerciseDao().upsert(custom)
            val bench = repository.getExercise("arete-barbell-bench-press")!!
            db.exerciseDao().upsert(bench.copy(isArchived = true))
            val archived = repository.getExercise(bench.id)!!
            assertTrue(archived.isArchived)
            assertNotNull(manifest().forExercise(archived))
            assertFalse(filterExercises(listOf(archived), "", null, null).any())
            assertEquals(custom, repository.getExercise("custom"))
            assertNull(repository.getExercise("absent"))
            assertEquals(46, db.exerciseDao().count())
        } finally { db.close() }
    }
}
