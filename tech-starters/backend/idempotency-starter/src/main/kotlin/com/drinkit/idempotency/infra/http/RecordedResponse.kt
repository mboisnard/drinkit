package com.drinkit.idempotency.infra.http

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal const val MAX_BODY_BYTES = 64 * 1024

/** What a replay answers: the status, the body and the headers that describe it, never a cookie. */
internal class RecordedResponse(
    private val status: Int,
    private val contentType: String,
    private val location: String,
    private val body: ByteArray,
) {
    fun replayOn(response: HttpServletResponse) {
        response.status = status
        if (contentType.isNotEmpty()) response.contentType = contentType
        if (location.isNotEmpty()) response.setHeader(HttpHeaders.LOCATION, location)
        response.outputStream.write(body)
    }

    fun toBytes(): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use {
            it.writeInt(status)
            it.writeUTF(contentType)
            it.writeUTF(location)
            it.write(body)
        }
    }.toByteArray()

    companion object {

        /** Null for an answer the next try may change: a server error, a request to slow down. */
        fun of(response: ContentCachingResponseWrapper): RecordedResponse? {
            if (!isFinal(response.status)) return null

            check(response.contentSize <= MAX_BODY_BYTES) {
                "An @Idempotent endpoint answers at most 64 KB, which a retry gets back"
            }

            return RecordedResponse(
                response.status,
                response.contentType.orEmpty(),
                response.getHeader(HttpHeaders.LOCATION).orEmpty(),
                response.contentAsByteArray,
            )
        }

        private fun isFinal(status: Int) = status < HttpStatus.INTERNAL_SERVER_ERROR.value() &&
            status != HttpStatus.TOO_MANY_REQUESTS.value()

        fun from(bytes: ByteArray) = DataInputStream(ByteArrayInputStream(bytes)).use {
            RecordedResponse(it.readInt(), it.readUTF(), it.readUTF(), it.readAllBytes())
        }
    }
}
