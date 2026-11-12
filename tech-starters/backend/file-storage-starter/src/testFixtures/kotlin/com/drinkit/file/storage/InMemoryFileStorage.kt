package com.drinkit.file.storage

import org.springframework.core.io.Resource
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Files kept in memory: [upload] stands for the frontend's upload, before a use case keeps the file. It checks no
 * reader: a business test proves its own [FileAccess].
 */
class InMemoryFileStorage : FileStorage {

    private val files = ConcurrentHashMap<FileId, File>()

    fun upload(usage: String): FileId = FileId(UUID.randomUUID().toString()).also { files[it] = File(usage) }

    /** The object a file is kept on, or null when it is not kept. */
    fun resourceOf(id: FileId): FileResource? = files[id]?.resource

    override fun store(usage: String, name: String, content: Resource): FileId = upload(usage)

    override fun keep(id: FileId, usage: String, resource: FileResource) {
        val temporary = File(usage)

        if (!files.replace(id, temporary, temporary.copy(resource = resource))) throw FileNotFound()
    }

    override fun delete(id: FileId) {
        val kept = files[id]?.takeIf { it.resource != null } ?: throw FileNotFound()

        if (!files.remove(id, kept)) throw FileNotFound()
    }

    override fun urlOf(id: FileId): URI = urlsOf(listOf(id))[id] ?: throw FileNotFound()

    override fun urlsOf(ids: Collection<FileId>): Map<FileId, URI> =
        ids.filter { resourceOf(it) != null }.associateWith { URI.create("memory:/files/${it.value}") }

    private data class File(val usage: String, val resource: FileResource? = null)
}
