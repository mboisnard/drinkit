package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileResource
import org.springframework.core.io.InputStreamSource
import org.springframework.http.ContentDisposition
import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.time.Instant

internal data class StoredFile(
    val id: FileId,
    val usage: String,
    val ownerId: String,
    val type: String,
    val size: Long,
    val name: String,
    val uploadedAt: Instant,
    val resource: FileResource? = null,
) {
    fun contentDisposition(): String = contentDispositionOf(type, name)
}

/** An image shows in the page, anything else downloads. */
internal fun contentDispositionOf(type: String, name: String): String {
    val image = type.startsWith("image/")
    val disposition = if (image) ContentDisposition.inline() else ContentDisposition.attachment()

    return disposition.filename(name, Charsets.UTF_8).build().toString()
}

/** What the application knows of each file, beside its bytes: an unknown id is a file not found. */
internal interface StoredFileMetadata {

    fun add(file: StoredFile)

    fun keep(id: FileId, ownerId: String, usage: String, resource: FileResource): Boolean

    fun markDeleted(id: FileId): Boolean

    fun findKept(ids: Collection<FileId>): List<StoredFile>

    /** The deleted files and the temporary ones uploaded too long ago, locked so that two cleanups never share one. */
    fun takeToCleanUp(uploadedBefore: Instant, limit: Int): List<FileId>

    fun removeAll(ids: List<FileId>)
}

/** Where the bytes live: the key of a file's object is its id. */
internal interface Storage {

    fun put(file: StoredFile, content: InputStreamSource)

    fun read(file: StoredFile): InputStream

    /** A missing object is not an error. */
    fun delete(ids: List<FileId>)

    fun urlOf(file: StoredFile, ttl: Duration): URI
}
