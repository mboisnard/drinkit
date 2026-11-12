package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID

internal abstract class StorageTestContract {

    abstract val storage: Storage

    /** The bytes a browser gets from the URL, or null when it is refused or finds nothing. */
    abstract fun readBack(url: URI): ByteArray?

    @Test
    fun `a stored file comes back identical through its URL`() {
        // Given
        val photo = aPhoto()

        // When
        storage.put(photo, content())

        // Then
        readBack(storage.urlOf(photo, TTL)) shouldBe BYTES
    }

    @Test
    fun `a stored file reads back identical`() {
        // Given
        val photo = aPhoto()

        // When
        storage.put(photo, content())

        // Then
        storage.read(photo).use { it.readAllBytes() } shouldBe BYTES
    }

    @Test
    fun `a deleted file is gone from its URL`() {
        // Given
        val photo = aPhoto()
        storage.put(photo, content())
        val url = storage.urlOf(photo, TTL)

        // When
        storage.delete(listOf(photo.id))

        // Then
        readBack(url).shouldBeNull()
    }

    @Test
    fun `deleting a missing file is not an error`() {
        // Given
        val missing = FileId(UUID.randomUUID().toString())

        // When
        storage.delete(listOf(missing))

        // Then
        readBack(storage.urlOf(aPhoto(id = missing), TTL)).shouldBeNull()
    }

    protected fun aPhoto(id: FileId = FileId(UUID.randomUUID().toString())) =
        StoredFile(id, "bottle-photo", "ada", "image/jpeg", BYTES.size.toLong(), "photo.jpg", Instant.EPOCH)

    protected fun content() = ByteArrayResource(BYTES)

    companion object {
        val TTL: Duration = Duration.ofMinutes(5)
        val BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)
    }
}
