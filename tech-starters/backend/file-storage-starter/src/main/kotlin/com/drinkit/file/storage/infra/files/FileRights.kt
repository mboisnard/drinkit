package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileAccess
import com.drinkit.file.storage.FileResource
import com.drinkit.security.ConnectedUser
import java.net.URI

internal const val PUBLIC_FILE_PATH = "/files/public/{id}"

/**
 * Who reads and deletes a kept file: anyone reads a public usage's, else its uploader, a read-all role, or whom the
 * [FileAccess] of its object allows. An object type with no FileAccess has no other reader nor deleter.
 */
internal class FileRights(
    private val publicUsages: Set<String>,
    private val readAllRoles: Set<String>,
    accesses: List<FileAccess>,
    private val contextPath: String = "",
) {

    private val accesses = accesses.associateBy { it.resourceType }

    fun isPublic(file: StoredFile): Boolean = file.usage in publicUsages

    fun publicUrlOf(file: StoredFile): URI = URI.create(contextPath + PUBLIC_FILE_PATH.replace("{id}", file.id.value))

    /** The files the reader may read, asking the FileAccess of each object once, however many files it holds. */
    fun readable(files: List<StoredFile>, reader: ConnectedUser?): List<StoredFile> {
        val asked = files.filterNot { isPublic(it) || it.isUploadedBy(reader) || reader.readsAll() }.toSet()
        val objects = asked.mapNotNull { it.resource }.toSet()
        val allowed = objects.filter { it.access()?.canRead(it.id, reader?.id) == true }.toSet()

        return files.filter { it !in asked || it.resource in allowed }
    }

    fun canDelete(file: StoredFile, user: ConnectedUser?): Boolean =
        file.isUploadedBy(user) || file.resource?.let { it.access()?.canDelete(it.id, user?.id) } == true

    private fun StoredFile.isUploadedBy(user: ConnectedUser?) = user != null && user.id == ownerId

    private fun ConnectedUser?.readsAll() = this != null && roles.any { it in readAllRoles }

    private fun FileResource.access() = accesses[type]
}
