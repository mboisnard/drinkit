package com.drinkit.idempotency.infra.http

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

/** Reads the body first, for its fingerprint, then hands it to the endpoint as if never read. */
internal class ReplayableBodyRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    // One byte past the limit tells a body too large, without reading the rest into memory
    val body: ByteArray = request.inputStream.readNBytes(MAX_BODY_BYTES + 1)

    val isTooLarge = body.size > MAX_BODY_BYTES

    override fun getInputStream(): ServletInputStream = object : ServletInputStream() {
        private val input = ByteArrayInputStream(body)

        override fun read() = input.read()

        override fun read(buffer: ByteArray, offset: Int, length: Int) = input.read(buffer, offset, length)

        override fun isFinished() = input.available() == 0

        override fun isReady() = true

        override fun setReadListener(listener: ReadListener) = throw UnsupportedOperationException()
    }

    override fun getReader() = BufferedReader(inputStream.reader(Charset.forName(characterEncoding ?: "UTF-8")))
}
