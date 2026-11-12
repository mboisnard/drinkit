package com.drinkit.file.storage.infra.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus

internal class UploadRulesTest {

    private val rules = UploadRules(
        mapOf("bottle-photo" to FileUsage(types = setOf("image/jpeg", "image/png"), maxSize = MAX_SIZE)),
    )

    @Test
    fun `an unknown usage is refused`() {
        // Given
        val photo = JPEG

        // When & Then
        shouldThrow<FileRefused> { accept("avatar", "photo.jpg", "image/jpeg", photo) }
            .statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `a file larger than its usage allows is refused`() {
        // Given
        val photo = JPEG + ByteArray(MAX_SIZE.toInt())

        // When & Then
        shouldThrow<FileRefused> { accept("bottle-photo", "photo.jpg", "image/jpeg", photo) }
            .statusCode shouldBe HttpStatus.CONTENT_TOO_LARGE
    }

    @Test
    fun `a file whose bytes are not a type of its usage is refused`() {
        // Given
        val document = PDF

        // When & Then
        shouldThrow<FileRefused> { accept("bottle-photo", "label.pdf", "application/pdf", document) }
            .statusCode shouldBe HttpStatus.UNSUPPORTED_MEDIA_TYPE
    }

    @Test
    fun `a file whose extension does not match its bytes is refused`() {
        // Given
        val photo = PNG

        // When & Then
        shouldThrow<FileRefused> { accept("bottle-photo", "photo.jpg", null, photo) }
            .statusCode shouldBe HttpStatus.UNSUPPORTED_MEDIA_TYPE
    }

    @Test
    fun `a file whose declared type does not match its bytes is refused`() {
        // Given
        val photo = PNG

        // When & Then
        shouldThrow<FileRefused> { accept("bottle-photo", "photo.png", "image/jpeg", photo) }
            .statusCode shouldBe HttpStatus.UNSUPPORTED_MEDIA_TYPE
    }

    @Test
    fun `a declared type that cannot be read is refused`() {
        // Given
        val photo = JPEG

        // When & Then
        shouldThrow<FileRefused> { accept("bottle-photo", "photo.jpg", "not a type", photo) }
            .statusCode shouldBe HttpStatus.UNSUPPORTED_MEDIA_TYPE
    }

    @Test
    fun `a refusal never repeats what the client sent`() {
        // Given
        val sent = "<script>".repeat(1000)

        // When
        val refusals = listOf(
            shouldThrow<FileRefused> { accept(sent, "photo.jpg", null, JPEG) },
            shouldThrow<FileRefused> { accept("bottle-photo", "$sent.png", null, JPEG) },
            shouldThrow<FileRefused> { accept("bottle-photo", "photo.jpg", "image/$sent", JPEG) },
        )

        // Then
        refusals.forEach { it.body.detail.orEmpty() shouldNotContain "<script>" }
    }

    @Test
    fun `a file whose size is unknown is refused`() {
        // Given
        val photo = ByteArrayResource(JPEG)

        // When & Then
        shouldThrow<FileRefused> { rules.accept("bottle-photo", "photo.jpg", null, size = -1, content = photo) }
            .statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `an accepted file keeps the type read from its bytes and only the safe characters of its name`() {
        // Given
        val photo = PNG

        // When
        val accepted = accept("bottle-photo", "../.Château Margaux 2015?.PNG", "image/png", photo)

        // Then
        accepted shouldBe AcceptedFile("image/png", "Ch_teau_Margaux_2015_.PNG")
    }

    private fun accept(usage: String, name: String, declaredType: String?, bytes: ByteArray) =
        rules.accept(usage, name, declaredType, bytes.size.toLong(), ByteArrayResource(bytes))

    private companion object {
        const val MAX_SIZE = 1024L

        val JPEG = byteArrayOf(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01)
        val PNG =
            byteArrayOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52)
        val PDF = "%PDF-1.7\n".toByteArray()

        fun byteArrayOf(vararg bytes: Int) = ByteArray(bytes.size) { bytes[it].toByte() }
    }
}
