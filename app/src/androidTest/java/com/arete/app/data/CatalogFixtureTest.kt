package com.arete.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arete.app.AreteApplication
import com.arete.app.catalog.EditorialManifest
import com.arete.app.catalog.filterExercises
import com.arete.app.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in disposable-AVD fixture; never inserts demos in production or the ordinary test suite. */
@RunWith(AndroidJUnit4::class)
class CatalogFixtureTest {
    @Test fun prepareOrCheckCatalogDevice() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("catalogFixture") ?: return@runBlocking
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AreteApplication
        if (mode == "seed") {
            app.automaticBackups.setEnabled(false)
            app.database.backupDao().replaceAll(BackupDatabaseData())
            app.repository.initialize()
            val now = System.currentTimeMillis()
            app.database.exerciseDao().upsert(ExerciseEntity("qa-custom-band", "Remo con banda casera", "Home Band Row", "Espalda",
                equipment = "Banda casera", instructions = "Mi instrucción personal: ancla la banda, fija el tronco y tira con control.",
                isCustom = true, createdAt = now, updatedAt = now))
            val id = app.sessionRepository.startFreeWorkout()
            app.sessionRepository.addExercise(id, "arete-barbell-bench-press")
            app.sessionRepository.addExercise(id, "arete-lat-pulldown")
            app.preferences.setThemeMode(ThemeMode.DARK)
        }
        if (mode == "light") app.preferences.setThemeMode(ThemeMode.LIGHT)
        if (mode == "dark") app.preferences.setThemeMode(ThemeMode.DARK)
        val catalog = app.database.exerciseDao().getAll()
        val manifest = app.assets.open("exercise_editorial.json").bufferedReader().use { EditorialManifest.decode(it.readText()) }
        assertEquals(46, catalog.size)
        assertEquals(45, catalog.count { manifest.forExercise(it) != null })
        assertEquals(9, catalog.count { manifest.forExercise(it)?.illustrations?.isNotEmpty() == true })
        assertEquals(listOf("qa-custom-band"), filterExercises(catalog, "home", "Espalda", "Banda casera").map { it.id })
        assertEquals("Mi instrucción personal: ancla la banda, fija el tronco y tira con control.", app.repository.getExercise("qa-custom-band")!!.instructions)
        val active = app.database.sessionDao().getActiveSession()!!
        assertEquals(2, active.exercises.size)
        assertEquals(6, active.exercises.sumOf { it.sets.size })
    }
}
