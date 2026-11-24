package com.drinkit.file.storage

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.drinkit.identifier.AbstractId
import com.drinkit.identifier.IdFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.ErrorResponseException
import java.net.URI

/**
 * The files uploaded through `POST /api/files`, or stored by the backend itself. A file not kept within the retention
 * is deleted, so a use case keeps it on its success path only.
 */
@TechStarterTool
interface FileStorage {

    /**
     * Stores a file the backend got itself, such as an importer's image, checked as an upload and temporary. Called
     * outside the transaction that keeps it, since it commits in its own: inside one, it holds a second connection.
     */
    fun store(usage: String, name: String, content: Resource): FileId

    /**
     * Keeps a temporary file of the current user, or of the backend with no user outside a request, on the business
     * object it belongs to, in the caller's transaction: a rollback leaves it temporary. A file of another usage passed
     * none of this usage's checks, so it is not found.
     */
    fun keep(id: FileId, usage: String, resource: FileResource)

    /**
     * Deletes a kept file, in the caller's transaction, for its uploader or whom the [FileAccess] of its object allows.
     * With no user outside a request, such as in a job, the backend deletes any.
     */
    fun delete(id: FileId)

    /**
     * A URL of a kept file: a stable one for a public usage, else a signed one valid a few minutes, for its uploader, a
     * read-all role, or whom the [FileAccess] of its object allows.
     */
    fun urlOf(id: FileId): URI

    /** The URLs of the files the reader may read among these, the others left out: one question per object. */
    fun urlsOf(ids: Collection<FileId>): Map<FileId, URI>
}

@TechStarterTool
class FileId(value: String) : AbstractId(value) {
    companion object : IdFactory<FileId>(::FileId)
}

data class FileResource(val type: String, val id: String)

@TechStarterTool
class FileNotFound : ErrorResponseException(NOT_FOUND) {
    init {
        setDetail("No such file")
    }
}
