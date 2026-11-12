package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileResource
import com.drinkit.file.storage.generated.jooq.DrinkitApplication
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.PRIVATE_USAGE
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.support.TransactionTemplate
import java.io.IOException
import java.nio.file.Path
import java.time.Duration

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
// Out of a web environment, no test runs in a request: the backend uploads, keeps and deletes the files
@SpringBootTest(classes = [FileStorageTestContext::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class FileCleanUpTest(
    private val files: StoredFiles,
    private val metadata: StoredFileMetadata,
    private val disk: Storage,
    private val transactions: TransactionTemplate,
    private val clock: ControlledClock,
    private val directory: Path,
) {

    private val cleanUp = FileCleanUp(metadata, disk, clock, RETENTION, transactions)

    @Test
    fun `a file kept in a transaction that rolls back is cleaned up after the retention`() {
        // Given
        val photo = uploaded()
        transactions.executeWithoutResult {
            files.keep(photo, PRIVATE_USAGE, EVENT)
            it.setRollbackOnly()
        }
        clock.add(RETENTION.plusSeconds(1))

        // When
        cleanUp.run()

        // Then
        directory.resolve(photo.value).shouldNotExist()
    }

    @Test
    fun `a file never kept stays within the retention`() {
        // Given
        val photo = uploaded()
        clock.add(RETENTION.minusSeconds(1))

        // When
        cleanUp.run()

        // Then
        directory.resolve(photo.value).shouldExist()
    }

    @Test
    fun `a kept file is never cleaned up`() {
        // Given
        val photo = uploaded()
        transactions.executeWithoutResult { files.keep(photo, PRIVATE_USAGE, EVENT) }
        clock.add(RETENTION.plusSeconds(1))

        // When
        cleanUp.run()

        // Then
        directory.resolve(photo.value).shouldExist()
    }

    @Test
    fun `a deleted file is cleaned up`() {
        // Given
        val photo = uploaded()
        transactions.executeWithoutResult { files.keep(photo, PRIVATE_USAGE, EVENT) }
        transactions.executeWithoutResult { files.delete(photo) }

        // When
        cleanUp.run()

        // Then
        directory.resolve(photo.value).shouldNotExist()
    }

    @Test
    fun `a cleanup that fails leaves its files to the next one`() {
        // Given
        val photo = uploaded()
        clock.add(RETENTION.plusSeconds(1))
        val brokenDisk = object : Storage by disk {
            override fun delete(ids: List<FileId>) = throw IOException("The disk is gone")
        }
        val failing = FileCleanUp(metadata, brokenDisk, clock, RETENTION, transactions)

        // When
        shouldNotThrowAny { failing.run() }
        cleanUp.run()

        // Then
        directory.resolve(photo.value).shouldNotExist()
    }

    private fun uploaded(): FileId = files.store(PRIVATE_USAGE, "photo.jpg", ByteArrayResource(JPEG))

    companion object {
        private val RETENTION = Duration.ofHours(24)
        private val EVENT = FileResource("event", "42")
        private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    }
}
