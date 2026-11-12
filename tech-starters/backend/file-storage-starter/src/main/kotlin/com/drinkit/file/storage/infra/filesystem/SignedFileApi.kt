package com.drinkit.file.storage.infra.filesystem

import com.drinkit.file.storage.infra.files.servedFile
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

/** Serves a file of the filesystem storage to whoever holds its signed URL, as a bucket serves a presigned one. */
internal class SignedFileApi(private val storage: FilesystemStorage, private val urlTtl: Duration) {

    fun serve(request: ServerRequest): ServerResponse {
        val file = SignedFile(
            id = request.pathVariable("id"),
            type = request.param("type").getOrNull().orEmpty(),
            name = request.param("name").getOrNull().orEmpty(),
            expires = request.param("expires").getOrNull()?.toLongOrNull() ?: 0,
            signature = request.param("signature").getOrNull().orEmpty(),
        )
        val path = storage.open(file) ?: throw ErrorResponseException(HttpStatus.FORBIDDEN)

        return servedFile(file.type, file.name, "private, max-age=${urlTtl.seconds}")
            .body(FileSystemResource(path))
    }
}

/** A file whose URL is signed: what the URL says of it cannot be changed without breaking the signature. */
internal data class SignedFile(
    val id: String,
    val type: String,
    val name: String,
    val expires: Long,
    val signature: String,
)
