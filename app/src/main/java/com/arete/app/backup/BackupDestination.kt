package com.arete.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.FileNotFoundException
import java.io.IOException

class BackupAccessException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Small SAF boundary: synchronous I/O is always called on Dispatchers.IO by the coordinator. */
interface BackupDestination {
    fun folderName(treeUri: String): String
    fun persistPermission(treeUri: String): Boolean
    fun hasPersistentPermission(treeUri: String): Boolean
    fun releasePermission(treeUri: String)
    fun create(treeUri: String, name: String): String
    fun write(uri: String, bytes: ByteArray)
    fun read(uri: String): ByteArray
    fun delete(uri: String): Boolean
}

class SafBackupDestination(context: Context) : BackupDestination {
    private val resolver = context.applicationContext.contentResolver
    private val grants = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    private fun parent(tree: String): Uri {
        val uri = Uri.parse(tree)
        return DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
    }

    override fun persistPermission(treeUri: String): Boolean {
        try { resolver.takePersistableUriPermission(Uri.parse(treeUri), grants) } catch (_: SecurityException) { /* Some providers only grant this session. */ }
        return hasPersistentPermission(treeUri)
    }

    override fun hasPersistentPermission(treeUri: String) = resolver.persistedUriPermissions.any {
        it.uri.toString() == treeUri && it.isReadPermission && it.isWritePermission
    }

    override fun releasePermission(treeUri: String) {
        runCatching { resolver.releasePersistableUriPermission(Uri.parse(treeUri), grants) }
    }

    override fun folderName(treeUri: String): String {
        try {
            resolver.query(parent(treeUri), arrayOf(Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) throw BackupAccessException("La carpeta de copias ya no existe. Selecciónala de nuevo.")
                if (cursor.getString(1) != Document.MIME_TYPE_DIR || cursor.getInt(2) and Document.FLAG_DIR_SUPPORTS_CREATE == 0)
                    throw BackupAccessException("La carpeta no permite crear copias. Elige otra ubicación.")
                return cursor.getString(0) ?: "Carpeta seleccionada"
            }
            throw IOException("El proveedor no pudo consultar la carpeta")
        } catch (error: SecurityException) { throw BackupAccessException("Se perdió el permiso de la carpeta. Selecciónala de nuevo.", error) }
        catch (error: FileNotFoundException) { throw BackupAccessException("La carpeta de copias ya no existe. Selecciónala de nuevo.", error) }
    }

    override fun create(treeUri: String, name: String): String {
        val tree = Uri.parse(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val existing = resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { cursor ->
            buildSet<String> { while (cursor.moveToNext()) add(cursor.getString(0)) }
        } ?: throw IOException("No se pudo comprobar el contenido de la carpeta")
        val created = DocumentsContract.createDocument(resolver, parent(treeUri), "application/json", name)
            ?: throw IOException("El proveedor no creó el archivo")
        if (DocumentsContract.getDocumentId(created) in existing) throw IOException("El proveedor devolvió un archivo existente; no se ha sobrescrito")
        return created.toString()
    }

    override fun write(uri: String, bytes: ByteArray) {
        val stream = resolver.openOutputStream(Uri.parse(uri), "w") ?: throw IOException("No se pudo abrir el archivo nuevo")
        stream.use { it.write(bytes); it.flush() }
    }
    override fun read(uri: String): ByteArray = resolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
        ?: throw IOException("No se pudo verificar el archivo guardado")
    override fun delete(uri: String): Boolean = DocumentsContract.deleteDocument(resolver, Uri.parse(uri))
}
