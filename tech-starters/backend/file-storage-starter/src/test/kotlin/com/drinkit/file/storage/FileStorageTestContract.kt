package com.drinkit.file.storage

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal abstract class FileStorageTestContract {

    abstract val storage: FileStorage

    /** A file the current user uploaded through the frontend for [USAGE], not kept yet. */
    abstract fun uploaded(): FileId

    /** The use case's transaction, which keep and delete join. */
    open fun <T> inTransaction(block: () -> T): T = block()

    @Test
    fun `a kept file has a URL`() {
        // Given
        val photo = uploaded()

        // When
        inTransaction { storage.keep(photo, USAGE, EVENT) }

        // Then
        shouldNotThrowAny { storage.urlOf(photo) }
    }

    @Test
    fun `a file uploaded for another usage is not kept`() {
        // Given
        val photo = uploaded()

        // When & Then
        shouldThrow<FileNotFound> { inTransaction { storage.keep(photo, "label-scan", EVENT) } }
    }

    @Test
    fun `a file is kept only once`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, EVENT) }

        // When & Then
        shouldThrow<FileNotFound> { inTransaction { storage.keep(photo, USAGE, EVENT) } }
    }

    @Test
    fun `a temporary file has no URL`() {
        // Given
        val photo = uploaded()

        // When & Then
        shouldThrow<FileNotFound> { storage.urlOf(photo) }
    }

    @Test
    fun `a deleted file has no URL`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, EVENT) }

        // When
        inTransaction { storage.delete(photo) }

        // Then
        shouldThrow<FileNotFound> { storage.urlOf(photo) }
    }

    @Test
    fun `the URLs of a list leave out the files not kept`() {
        // Given
        val kept = uploaded()
        val temporary = uploaded()
        inTransaction { storage.keep(kept, USAGE, EVENT) }

        // When
        val urls = storage.urlsOf(listOf(kept, temporary))

        // Then
        urls.keys shouldBe setOf(kept)
    }

    @Test
    fun `an unknown file is not found`() {
        // Given
        val unknown = FileId("not-a-file")

        // When & Then
        shouldThrow<FileNotFound> { inTransaction { storage.keep(unknown, USAGE, EVENT) } }
    }

    companion object {
        const val USAGE = "bottle-photo"
        val EVENT = FileResource("event", "42")
    }
}
