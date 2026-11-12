package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileNotFound
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.CurrentUser
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

internal const val UPLOAD_PATH = "/api/files"

// Longer than any usage name, so a longer one is refused as unknown without being read whole
private const val MAX_USAGE_BYTES = 64

// A file id never changes its bytes, so a day of cache only delays a deletion
private const val PUBLIC_CACHE = "public, max-age=86400"

internal class FileUploadApi(
    private val uploads: FileUploads,
    private val cleanUp: FileCleanUp,
    private val rateLimiter: RateLimiter,
    private val uploadLimit: RateLimitRule,
    private val currentUser: CurrentUser,
) {

    // Counted before the body is read, so that a limited user costs no parsing
    fun upload(request: ServerRequest): ServerResponse {
        rateLimiter.consume(uploadLimit, currentUser.get().id).orThrow()

        val parts = request.multipartData()
        val usage = parts.getFirst(
            "usage",
        )?.inputStream?.use { it.readNBytes(MAX_USAGE_BYTES).toString(Charsets.UTF_8) }
        val file = parts.getFirst("file")
        if (usage == null || file == null) throw FileRefused(BAD_REQUEST, "An upload holds a file and its usage")

        // The container already holds the part, and opens a new stream on it at each call
        val content = InputStreamSource { file.inputStream }
        val id = uploads.upload(usage, file.submittedFileName.orEmpty(), file.contentType, file.size, content)

        cleanUp.run()

        return ServerResponse.status(CREATED).body(mapOf("id" to id.value))
    }
}

/** Serves the kept files of a public usage to anyone, through the application whatever the storage. */
internal class PublicFileApi(private val files: StoredFiles, private val storage: Storage) {

    fun serve(request: ServerRequest): ServerResponse {
        val file = files.findPublic(FileId(request.pathVariable("id"))) ?: throw FileNotFound()

        return servedFile(file.type, file.name, PUBLIC_CACHE)
            .contentLength(file.size)
            .body(InputStreamResource(storage.read(file)))
    }
}

/**
 * A file served so that a browser never runs it, whatever it holds.
 *
 * OWASP: File Upload Cheat Sheet (Content-Disposition, X-Content-Type-Options: nosniff, a sandboxed CSP).
 */
internal fun servedFile(type: String, name: String, cacheControl: String): ServerResponse.BodyBuilder =
    ServerResponse.ok()
        .contentType(MediaType.parseMediaType(type))
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionOf(type, name))
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .header(HttpHeaders.CACHE_CONTROL, cacheControl)
