package com.arete.app.backup

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.automaticBackupDataStore by preferencesDataStore("automatic_backup")

@Serializable
data class ManagedBackup(val treeUri: String, val uri: String, val createdAt: Long, val sha256: String)

@Serializable
enum class BackupOutcome { NONE, RUNNING, SUCCESS, ERROR, INTERRUPTED }

/** Device-local state, deliberately excluded from Backup JSON v1 and Android transfer. */
@Serializable
data class AutomaticBackupState(
    val enabled: Boolean = false,
    val treeUri: String? = null,
    val folderName: String = "",
    val lastSuccessAt: Long? = null,
    val lastSuccessFolder: String = "",
    val lastSuccessName: String = "",
    val lastSuccessAutomatic: Boolean = false,
    val outcome: BackupOutcome = BackupOutcome.NONE,
    val detail: String = "",
    val managed: List<ManagedBackup> = emptyList()
)

interface AutomaticBackupStore {
    val state: Flow<AutomaticBackupState>
    suspend fun update(change: (AutomaticBackupState) -> AutomaticBackupState)
}

class AutomaticBackupPreferences(context: Context) : AutomaticBackupStore {
    private val store = context.applicationContext.automaticBackupDataStore
    private val key = stringPreferencesKey("state_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun decode(raw: String?) = if (raw == null) AutomaticBackupState() else runCatching {
        json.decodeFromString<AutomaticBackupState>(raw)
    }.getOrElse { AutomaticBackupState(outcome = BackupOutcome.ERROR,
        detail = "No se pudo leer la configuración local. Selecciona de nuevo la carpeta; las copias externas no se han borrado.") }

    override val state = store.data.map { decode(it[key]) }
    override suspend fun update(change: (AutomaticBackupState) -> AutomaticBackupState) {
        store.edit { it[key] = json.encodeToString(change(decode(it[key]))) }
    }
}
