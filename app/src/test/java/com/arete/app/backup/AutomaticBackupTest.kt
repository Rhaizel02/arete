package com.arete.app.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.*
import com.arete.app.data.local.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutomaticBackupTest {
    private lateinit var db: AreteDatabase
    private lateinit var backup: BackupService
    private lateinit var preferences: PreferencesRepository
    private lateinit var manager: AutomaticBackupCoordinator
    private val store = MemoryStore()
    private val files = FakeDestination()
    private val scheduling = FakeScheduling()
    private var clock = 1_800_000_000_000L
    private var id = 0

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        preferences = PreferencesRepository(context)
        preferences.restore(BackupPreferences(weeklyGoal = 5))
        db.exerciseDao().upsert(ExerciseEntity("bench", "Press", "Bench", "Pecho", equipment = "Barra", createdAt = 1, updatedAt = 1))
        backup = BackupService(db, preferences, now = { clock })
        manager = AutomaticBackupCoordinator(backup, store, files, scheduling, { clock++ }, { "copy-${id++}" })
        manager.selectFolder("tree-a", true)
        Unit
    }
    @After fun close() = db.close()

    @Test fun realV1RoundTripAndManualFilesNeverEnterAutomaticRetention() = runTest {
        assertTrue(manager.createCopy(false))
        val manual = files.content.keys.single()
        val payload = files.content.getValue(manual).toString(Charsets.UTF_8)
        assertEquals(1, backup.decodeAndValidate(payload).backupSchemaVersion)
        assertTrue(store.state.value.managed.isEmpty())
        repeat(10) { assertTrue(manager.createCopy(true)) }
        assertEquals(7, store.state.value.managed.size)
        assertTrue(files.content.containsKey(manual))
        db.backupDao().replaceAll(BackupDatabaseData())
        preferences.restore(BackupPreferences())
        manager.restore(payload)
        assertEquals("bench", db.backupDao().exercises().single().id)
        assertEquals(5, preferences.settings.first().weeklyGoal)
        assertTrue(store.state.value.enabled)
    }

    @Test fun retainsSevenAndNeverDeletesForeignEvenWithAnAutomaticLookingName() = runTest {
        files.content["foreign"] = "un documento ajeno".toByteArray()
        files.content["tree-a/Arete_backup_auto_2026-10-23.json"] = backup.export().toByteArray()
        repeat(9) { manager.createCopy(true) }
        assertEquals(7, store.state.value.managed.size)
        assertEquals(9, files.content.size)
        assertEquals(2, files.deleted.size)
        assertTrue(files.content.containsKey("foreign"))
        assertTrue(files.content.containsKey("tree-a/Arete_backup_auto_2026-10-23.json"))
        assertTrue(store.state.value.managed.all { AutomaticBackupCoordinator.sha256(files.content.getValue(it.uri)) == it.sha256 })
        clock -= 86_400_000
        assertTrue(manager.createCopy(true))
        assertTrue(files.content.containsKey(store.state.value.managed.last().uri))
        assertEquals(7, store.state.value.managed.size)
    }

    @Test fun writeCloseAndReadbackFailuresPreserveAllPreviousCopiesAndSuccessTimestamp() = runTest {
        repeat(7) { manager.createCopy(true) }
        val old = files.content.mapValues { it.value.clone() }
        val success = store.state.value.lastSuccessAt
        listOf("partial", "close", "readback").forEach { failure ->
            files.failure = failure
            assertFalse(manager.createCopy(true))
            old.forEach { (uri, bytes) -> assertArrayEquals(bytes, files.content.getValue(uri)) }
            assertEquals(success, store.state.value.lastSuccessAt)
            assertEquals(7, store.state.value.managed.size)
            assertEquals(BackupOutcome.ERROR, store.state.value.outcome)
        }
    }

    @Test fun modifiedOrUndeletableFilesAreConservedWithWarningAndFolderChangeDoesNotTouchOldFolder() = runTest {
        repeat(7) { manager.createCopy(true) }
        val changed = store.state.value.managed.first().uri
        files.content[changed] = "contenido cambiado externamente".toByteArray()
        assertTrue(manager.createCopy(true))
        assertTrue(files.content.containsKey(changed))
        assertTrue(store.state.value.detail.contains("No se limpiaron"))
        files.denyDelete = true
        manager.selectFolder("tree-b", true)
        repeat(8) { manager.createCopy(true) }
        assertEquals(8, files.content.keys.count { it.startsWith("tree-a/") })
        assertEquals(8, files.content.keys.count { it.startsWith("tree-b/") })
        assertTrue(store.state.value.detail.contains("más de siete"))
        files.denyDelete = false
        manager.createCopy(true)
        assertEquals(7, files.content.keys.count { it.startsWith("tree-b/") })
        assertTrue(files.content.containsKey(changed))
    }

    @Test fun revokedOrMissingDestinationDisablesDailyButTransientFailureDoesNot() = runTest {
        manager.createCopy(true)
        val success = store.state.value.lastSuccessAt
        files.failure = "folder-io"
        assertFalse(manager.createCopy(true))
        assertTrue(store.state.value.enabled)
        files.failure = "missing"
        assertFalse(manager.createCopy(true))
        assertFalse(store.state.value.enabled)
        assertFalse(scheduling.enabled)
        assertEquals(success, store.state.value.lastSuccessAt)
        files.failure = null
        manager.setEnabled(true)
        files.persistent = false
        manager.reconcile()
        assertFalse(store.state.value.enabled)
        assertFalse(scheduling.enabled)
        manager.selectFolder("tree-b", true)
        assertFalse(store.state.value.enabled)
        assertTrue(manager.createCopy(false)) // A session grant is sufficient for an immediate copy.
    }

    @Test fun disabledWorkerDoesNotWriteAndReinstallNeverAdoptsOldFilesForCleanup() = runTest {
        repeat(7) { manager.createCopy(true) }
        manager.setEnabled(false)
        val previous = files.content.keys.toSet()
        assertTrue(manager.createCopy(true))
        assertEquals(previous, files.content.keys)
        val freshStore = MemoryStore()
        val reinstalled = AutomaticBackupCoordinator(backup, freshStore, files, scheduling, { clock++ }, { "new-${id++}" })
        reinstalled.restore(backup.export())
        assertFalse(freshStore.state.value.enabled)
        assertNull(freshStore.state.value.treeUri)
        reinstalled.selectFolder("tree-a", true)
        repeat(8) { reinstalled.createCopy(true) }
        assertTrue(previous.all { it in files.content })
        assertEquals(14, files.content.size)
    }

    @Test fun cancellationAndConcurrentRequestsNeverDeleteOlderCopiesOrOverlapWrites() = runTest {
        repeat(7) { manager.createCopy(true) }
        val old = files.content.keys.toSet()
        files.failure = "cancel"
        try { manager.createCopy(true); fail("Cancellation must propagate") } catch (_: CancellationException) { }
        assertTrue(old.all { it in files.content })
        assertEquals(BackupOutcome.INTERRUPTED, store.state.value.outcome)
        files.failure = null
        coroutineScope { repeat(4) { launch(Dispatchers.Default) { manager.createCopy(true) } } }
        assertEquals(1, files.maximumConcurrentWrites)
        assertEquals(7, store.state.value.managed.size)
    }

    @Test fun providerReturningExistingManagedFileCannotOverwriteOrDeleteIt() = runTest {
        manager.createCopy(true)
        val first = store.state.value.managed.single()
        val bytes = files.content.getValue(first.uri).clone()
        files.returnExisting = first.uri
        assertFalse(manager.createCopy(true))
        assertArrayEquals(bytes, files.content.getValue(first.uri))
        assertTrue(files.deleted.isEmpty())
    }

    @Test fun failedLocalRegistrationKeepsVerifiedFileAndNeverRunsRetention() = runTest {
        repeat(7) { manager.createCopy(true) }
        val previous = files.content.keys.toSet()
        store.failSuccess = true
        assertFalse(manager.createCopy(true))
        assertEquals(8, files.content.size)
        assertTrue(previous.all { it in files.content })
        assertTrue(files.deleted.isEmpty())
        assertEquals(7, store.state.value.managed.size)
    }

    @Test fun schedulingFailureNeverLeavesAnEnabledButUnscheduledConfiguration() = runTest {
        scheduling.fail = true
        try { manager.selectFolder("tree-b", true); fail("Scheduling should fail") } catch (_: IOException) { }
        assertFalse(store.state.value.enabled)
        assertEquals(BackupOutcome.ERROR, store.state.value.outcome)
        assertTrue(store.state.value.detail.contains("programación"))
        scheduling.fail = false
        manager.setEnabled(true)
        assertTrue(store.state.value.enabled)
    }

    private class MemoryStore : AutomaticBackupStore {
        override val state = MutableStateFlow(AutomaticBackupState())
        var failSuccess = false
        override suspend fun update(change: (AutomaticBackupState) -> AutomaticBackupState) {
            val next = change(state.value)
            if (failSuccess && next.outcome == BackupOutcome.SUCCESS) throw IOException("DataStore unavailable")
            state.value = next
        }
    }
    private class FakeScheduling : BackupWorkScheduling {
        var enabled = false
        var fail = false
        override suspend fun daily(enabled: Boolean) { if (fail) throw IOException("Scheduler unavailable"); this.enabled = enabled }
        override suspend fun requestNow() { }
    }
    private class FakeDestination : BackupDestination {
        val content = linkedMapOf<String, ByteArray>()
        val deleted = mutableListOf<String>()
        var failure: String? = null
        var persistent = true
        var denyDelete = false
        var returnExisting: String? = null
        var latest = ""
        var writes = 0
        var maximumConcurrentWrites = 0
        override fun folderName(treeUri: String): String {
            if (failure == "missing") throw BackupAccessException("La carpeta no existe")
            if (failure == "folder-io") throw IOException("Provider temporarily offline")
            return treeUri
        }
        override fun persistPermission(treeUri: String) = persistent
        override fun hasPersistentPermission(treeUri: String) = persistent
        override fun releasePermission(treeUri: String) { }
        override fun create(treeUri: String, name: String): String {
            returnExisting?.let { return it }
            latest = "$treeUri/$name"
            content[latest] = byteArrayOf()
            return latest
        }
        override fun write(uri: String, bytes: ByteArray) {
            writes++
            maximumConcurrentWrites = maxOf(maximumConcurrentWrites, writes)
            try {
                Thread.sleep(5) // Make an accidental concurrent writer observable.
                content[uri] = if (failure == "partial") bytes.take(20).toByteArray() else bytes.clone()
                if (failure == "cancel") throw CancellationException("Work stopped")
                if (failure in listOf("partial", "close")) throw IOException("Injected $failure failure")
            } finally { writes-- }
        }
        override fun read(uri: String): ByteArray = if (failure == "readback" && uri == latest) "invalid".toByteArray() else content.getValue(uri)
        override fun delete(uri: String): Boolean {
            if (denyDelete) return false
            deleted += uri
            return content.remove(uri) != null
        }
    }
}
