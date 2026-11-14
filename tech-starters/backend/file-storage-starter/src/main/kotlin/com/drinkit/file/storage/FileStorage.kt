package com.drinkit.file.storage

import com.drinkit.documentation.tech.starter.TechStarterTool
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

data class FileId(val value: String)

/** The business object a file is kept on, such as event 42: its readers are the file's readers. */
data class FileResource(val type: String, val id: String)

/**
 * Who may read, and delete, the files kept on one type of business object, beside their uploader. One bean per type,
 * which asks the same rule as the use cases of that object; none means nobody else.
 */
@TechStarterTool
interface FileAccess {

    val resourceType: String

    /** [readerId] is null for an anonymous reader. */
    fun canRead(resourceId: String, readerId: String?): Boolean

    /** Only the uploader deletes a file, unless the object says otherwise. */
    fun canDelete(resourceId: String, userId: String?): Boolean = false
}

/** Unknown, someone else's, of another usage, not kept or already kept: one answer, so an id tells nothing. */
@TechStarterTool
class FileNotFound : ErrorResponseException(NOT_FOUND) {
    init {
        setDetail("No such file")
    }
}
