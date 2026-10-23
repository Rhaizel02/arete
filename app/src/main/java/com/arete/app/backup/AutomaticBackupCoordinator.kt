package com.arete.app.backup

import com.arete.app.data.BackupService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.IOException

/** One process-wide coordinator for file creation, configuration and explicit restoration. */
class AutomaticBackupCoordinator(
    private val backup: BackupService,
    private val store: AutomaticBackupStore,
    private val destination: BackupDestination,
    private val scheduling: BackupWorkScheduling,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val mutex = Mutex()

    suspend fun selectFolder(treeUri: String, enable: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val label = destination.folderName(treeUri)
            val persistent = destination.persistPermission(treeUri)
            val previous = store.state.first()
            val enabled = (enable || previous.enabled) && persistent
            store.update { it.copy(treeUri = treeUri, folderName = label, enabled = enabled,
                detail = if (persistent) "Carpeta configurada. Las copias existentes se conservan."
                else "El proveedor no ofrece permiso persistente. Puedes crear una copia ahora; el backup diario está desactivado.") }
            applySchedule(enabled)
            previous.treeUri?.takeIf { it != treeUri }?.let(destination::releasePermission)
            Unit
        }
    }

    suspend fun setEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (enabled) {
                val tree = store.state.first().treeUri ?: throw BackupAccessException("Selecciona primero una carpeta")
                if (!destination.hasPersistentPermission(tree)) throw BackupAccessException("Selecciona de nuevo la carpeta para conceder acceso persistente")
                destination.folderName(tree)
            }
            // Save intent first: a surviving worker checks this flag before any write.
            store.update { it.copy(enabled = enabled) }
            applySchedule(enabled)
        }
    }

    suspend fun requestNow() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tree = store.state.first().treeUri ?: throw BackupAccessException("Selecciona primero una carpeta")
            destination.folderName(tree)
            scheduling.requestNow()
        }
    }

    suspend fun reconcile(recoverInterrupted: Boolean = false) = withContext(Dispatchers.IO) {
        mutex.withLock { reconcileLocked(recoverInterrupted) }
    }

    private suspend fun reconcileLocked(recoverInterrupted: Boolean = false) {
        var state = store.state.first()
        if (recoverInterrupted && state.outcome == BackupOutcome.RUNNING) {
            store.update { it.copy(outcome = BackupOutcome.INTERRUPTED, detail = "La ejecución anterior se interrumpió. No se ha confirmado una copia nueva.") }
        }
        if (state.treeUri != null) {
            try {
                if (state.enabled && !destination.hasPersistentPermission(state.treeUri)) throw BackupAccessException("Se perdió el permiso persistente. Selecciona de nuevo la carpeta y activa el backup diario.")
                destination.folderName(state.treeUri)
            } catch (error: Exception) {
                state = state.copy(enabled = state.enabled && !isAccessError(error))
                store.update { it.copy(enabled = state.enabled, outcome = BackupOutcome.ERROR, detail = readableError(error)) }
            }
        } else if (state.enabled) {
            state = state.copy(enabled = false)
            store.update { it.copy(enabled = false) }
        }
        applySchedule(state.enabled)
    }

    private suspend fun applySchedule(enabled: Boolean) {
        try { scheduling.daily(enabled) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            store.update { it.copy(enabled = false, outcome = BackupOutcome.ERROR,
                detail = "No se pudo actualizar la programación: ${error.message.orEmpty()}") }
            throw error
        }
    }

    suspend fun restore(raw: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            backup.restore(raw)
            // The JSON never replaces device-local destination or permission state.
            try { reconcileLocked() } catch (error: Exception) {
                store.update { it.copy(outcome = BackupOutcome.ERROR, detail = "Datos restaurados; revisa la programación de copias: ${error.message.orEmpty()}") }
            }
        }
    }

    suspend fun createCopy(automatic: Boolean): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val initial = store.state.first()
            if (automatic && !initial.enabled) return@withLock true
            var created: String? = null
            var verified = false
            try {
                store.update { it.copy(outcome = BackupOutcome.RUNNING, detail = "Creando y verificando una copia ${if (automatic) "automática" else "manual"}…") }
                val tree = initial.treeUri ?: throw BackupAccessException("Selecciona una carpeta para guardar tus copias")
                if (automatic && !destination.hasPersistentPermission(tree)) throw BackupAccessException("Se perdió el permiso persistente. Selecciona de nuevo la carpeta.")
                val folder = destination.folderName(tree)
                val raw = backup.export()
                backup.decodeAndValidate(raw)
                val bytes = raw.toByteArray(Charsets.UTF_8)
                val digest = sha256(bytes)
                val timestamp = now()
                val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss_SSS", Locale.ROOT).format(Date(timestamp))
                val name = "Arete_backup_${if (automatic) "auto" else "manual"}_${stamp}_${newId()}.json"
                created = destination.create(tree, name)
                // Never allow a provider to return a previously managed file for writing.
                if (initial.managed.any { it.uri == created }) { created = null; throw IOException("El proveedor devolvió una copia existente; no se ha sobrescrito") }
                destination.write(created, bytes)
                val readBack = destination.read(created)
                if (readBack.size != bytes.size || sha256(readBack) != digest) throw IOException("La copia leída no coincide con el contenido exportado")
                backup.decodeAndValidate(readBack.toString(Charsets.UTF_8))
                verified = true
                val managed = ManagedBackup(tree, created, timestamp, digest)
                store.update { it.copy(lastSuccessAt = timestamp, lastSuccessFolder = folder, lastSuccessName = name,
                    lastSuccessAutomatic = automatic, outcome = BackupOutcome.SUCCESS, detail = "Copia ${if (automatic) "automática" else "manual"} verificada. La sincronización remota depende del proveedor.",
                    managed = if (automatic) it.managed + managed else it.managed) }
                if (automatic) {
                    val warning = retainSeven(tree)
                    if (warning != null) store.update { it.copy(detail = "Copia verificada. $warning") }
                }
                true
            } catch (error: CancellationException) {
                withContext(NonCancellable) {
                    if (!verified) created?.let { runCatching { destination.delete(it) } }
                    store.update { it.copy(outcome = BackupOutcome.INTERRUPTED, detail = "Ejecución interrumpida. Se conservan las copias anteriores.") }
                }
                throw error
            } catch (error: Exception) {
                if (!verified) created?.let { runCatching { destination.delete(it) } }
                val lostAccess = isAccessError(error)
                store.update { it.copy(enabled = it.enabled && !lostAccess, outcome = BackupOutcome.ERROR,
                    detail = readableError(error) + if (verified) " El archivo nuevo se conserva, pero no se pudo completar el registro local." else " No se ha confirmado una copia nueva; las anteriores se conservan.") }
                if (lostAccess) applySchedule(false)
                false
            }
        }
    }

    private suspend fun retainSeven(tree: String): String? {
        // Verified copies are appended under the mutex. Insertion order remains reliable even
        // if the device clock moves backwards: never delete the just-verified latest copy.
        val known = store.state.first().managed.filter { it.treeUri == tree }.asReversed()
        if (known.size <= 7) return null
        try {
            // A changed or unreadable file is never deleted or counted as a verified survivor.
            known.forEach { copy ->
                val bytes = destination.read(copy.uri)
                if (sha256(bytes) != copy.sha256) return "No se limpiaron copias anteriores: un archivo cambió fuera de Arete."
                backup.decodeAndValidate(bytes.toString(Charsets.UTF_8))
            }
            known.drop(7).forEach { copy ->
                if (sha256(destination.read(copy.uri)) != copy.sha256 || !destination.delete(copy.uri))
                    return "No se pudieron retirar todas las copias antiguas; se conservan más de siete."
                store.update { it.copy(managed = it.managed.filterNot { entry -> entry.uri == copy.uri }) }
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { return "No se pudieron verificar o retirar copias antiguas; se han conservado." }
        return null
    }

    private fun isAccessError(error: Exception) = error is BackupAccessException || error is SecurityException || error is java.io.FileNotFoundException
    private fun readableError(error: Exception) = if (isAccessError(error)) error.message ?: "Se perdió el acceso a la carpeta. Selecciónala de nuevo."
        else "No se pudo completar la operación con el proveedor: ${error.message ?: "error de lectura o escritura"}"

    companion object {
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
