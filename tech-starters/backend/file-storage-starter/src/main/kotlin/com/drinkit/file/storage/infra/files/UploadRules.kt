package com.drinkit.file.storage.infra.files

import org.apache.tika.Tika
import org.apache.tika.mime.MimeTypes
import org.springframework.core.io.InputStreamSource
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONTENT_TOO_LARGE
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.MediaType
import org.springframework.web.ErrorResponseException

private const val MAX_NAME_LENGTH = 255

internal data class FileUsage(val types: Set<String>, val maxSize: Long)

internal data class AcceptedFile(val type: String, val name: String)

/**
 * The type comes from the bytes, never from the client: the name's extension and the declared type must agree with it.
 *
 * OWASP: File Upload Cheat Sheet (extension and content-type validation, file signature, filename sanitization).
 */
internal class UploadRules(private val usages: Map<String, FileUsage>) {

    private val tika = Tika()

    fun accept(
        usage: String,
        name: String,
        declaredType: String?,
        size: Long,
        content: InputStreamSource,
    ): AcceptedFile {
        val allowed = usages[usage] ?: throw FileRefused(BAD_REQUEST, "No such file usage")
        val type = content.inputStream.buffered().use(tika::detect)

        // Built, then thrown once: detekt allows two throws per function
        val refusal = when {
            size < 0 -> FileRefused(BAD_REQUEST, "The size of the file is unknown")

            size > allowed.maxSize -> FileRefused(CONTENT_TOO_LARGE, "A $usage holds at most ${allowed.maxSize} bytes")

            type !in allowed.types -> FileRefused(UNSUPPORTED_MEDIA_TYPE, "A $usage cannot be a $type")

            !name.hasExtensionOf(type) ->
                FileRefused(UNSUPPORTED_MEDIA_TYPE, "The extension of the file name is not one of a $type")

            declaredType != null && declaredType.withoutParameters() != type ->
                FileRefused(UNSUPPORTED_MEDIA_TYPE, "The declared type is not the type of the file, a $type")

            else -> null
        }
        refusal?.let { throw it }

        return AcceptedFile(type, name.safe())
    }

    private fun String.hasExtensionOf(type: String): Boolean {
        val extension = "." + substringAfterLast('.', "").lowercase()

        return extension in MimeTypes.getDefaultMimeTypes().forName(type).extensions
    }

    private fun String.withoutParameters() =
        runCatching { MediaType.parseMediaType(this) }.getOrNull()?.let { "${it.type}/${it.subtype}" }

    private fun String.safe(): String = substringAfterLast('/')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trimStart('.', '-', '_')
        .take(MAX_NAME_LENGTH)
}

/** An upload refused before anything is stored. Its detail never repeats what the client sent. */
internal class FileRefused(status: HttpStatus, detail: String) : ErrorResponseException(status) {
    init {
        setDetail(detail)
    }
}
