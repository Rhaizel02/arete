package com.arete.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.arete.app.AreteApplication
import com.arete.app.backup.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in only. Operates on the SAF folder selected in this disposable AVD. */
@RunWith(AndroidJUnit4::class)
class BackupDeviceTest {
    @Test fun validateBackupDevice() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("backupFixture") ?: return@runBlocking
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as AreteApplication
        val destination = SafBackupDestination(context)
        val initial = app.automaticBackupPreferences.state.first()
        when (mode) {
            "retention" -> {
                assertTrue("Configure and enable automatic backup in Settings first", initial.enabled)
                val tree = initial.treeUri!!
                val foreign = destination.create(tree, "Arete_backup_auto_foreign-test.json")
                destination.write(foreign, "Documento de prueba ajeno a la retención".toByteArray())
                assertTrue(app.automaticBackups.createCopy(false))
                val before = app.backupService.decodeAndValidate(app.backupService.export())
                repeat(9) {
                    val worker = TestListenableWorkerBuilder<BackupWorker>(context)
                        .setInputData(workDataOf(BackupWorkScheduler.AUTOMATIC to true)).build()
                    assertEquals(ListenableWorker.Result.success(), worker.doWork())
                }
                val after = app.automaticBackupPreferences.state.first()
                val managed = after.managed.filter { it.treeUri == tree }
                assertEquals(7, managed.size)
                managed.forEach {
                    val bytes = destination.read(it.uri)
                    assertEquals(it.sha256, AutomaticBackupCoordinator.sha256(bytes))
                    val copy = app.backupService.decodeAndValidate(bytes.toString(Charsets.UTF_8))
                    assertEquals(before.database, copy.database)
                    assertEquals(before.preferences, copy.preferences)
                }
                assertTrue(destination.read(foreign).toString(Charsets.UTF_8).startsWith("Documento"))
                println("BACKUP_DEVICE: verified=${managed.size}; latest=${after.lastSuccessName}; folder=$tree; rows=${before.database.workoutSessions.size} sessions/${before.database.sessionSets.size} sets/${before.database.bodyWeights.size} weights")
            }
            "revoke" -> {
                val success = initial.lastSuccessAt
                destination.releasePermission(initial.treeUri!!)
                app.automaticBackups.reconcile()
                val after = app.automaticBackupPreferences.state.first()
                assertFalse(after.enabled)
                assertEquals(success, after.lastSuccessAt)
                assertEquals(BackupOutcome.ERROR, after.outcome)
                println("BACKUP_DEVICE: access revoked; daily disabled; previous success preserved")
            }
            "restored" -> {
                val copy = app.backupService.decodeAndValidate(app.backupService.export())
                assertEquals(1, copy.backupSchemaVersion)
                assertEquals(1, copy.database.routines.size)
                assertEquals(4, copy.database.exercises.size)
                assertEquals(8, copy.database.workoutSessions.size)
                assertEquals(128, copy.database.sessionSets.size)
                assertEquals(7, copy.database.bodyWeights.size)
                assertEquals(3, copy.preferences.weeklyGoal)
                assertEquals("DARK", copy.preferences.themeMode)
                assertFalse(app.automaticBackupPreferences.state.first().enabled)
                assertNull(app.automaticBackupPreferences.state.first().treeUri)
                println("BACKUP_DEVICE: routines/sessions/sets/weights/preferences restored; automatic backup remains unconfigured")
            }
            else -> error("Unknown opt-in backup validation mode")
        }
    }
}
