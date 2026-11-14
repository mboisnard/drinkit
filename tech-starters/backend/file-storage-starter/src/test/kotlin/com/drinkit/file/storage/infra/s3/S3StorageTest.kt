package com.drinkit.file.storage.infra.s3

import com.drinkit.file.storage.RustfsIntegrationTest
import com.drinkit.file.storage.S3Server
import com.drinkit.file.storage.infra.files.StorageTestContract
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@RustfsIntegrationTest
internal class S3StorageTest(server: S3Server) : StorageTestContract() {

    private val settings = S3Settings(server.endpoint, "auto", server.bucket, server.accessKey, server.secretKey)

    override val storage = S3Storage.of(settings)

    private val http = HttpClient.newHttpClient()

    override fun readBack(url: URI): ByteArray? {
        val response = http.send(HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray())

        return response.body().takeIf { response.statusCode() == 200 }
    }

    @Test
    fun `a URL expires after its time to live`() {
        // Given
        val photo = aPhoto()
        storage.put(photo, content())

        // When
        val url = storage.urlOf(photo, TTL)

        // Then
        url.query shouldContain "X-Amz-Expires=${TTL.seconds}"
    }
}
