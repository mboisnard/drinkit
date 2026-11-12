package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileNotFound
import com.drinkit.file.storage.FileResource
import com.drinkit.file.storage.FileStorageTestContract
import com.drinkit.file.storage.generated.jooq.DrinkitApplication
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.ADA
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.GRACE
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.PUBLIC_USAGE
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.InMemoryCurrentUser
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@SpringBootTest(classes = [FileStorageTestContext::class])
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class StoredFilesTest(
    override val storage: StoredFiles,
    private val uploads: FileUploads,
    private val transactions: TransactionTemplate,
    private val user: InMemoryCurrentUser,
    private val directory: Path,
) : FileStorageTestContract() {

    @BeforeEach
    fun signInAdaInARequest() {
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest()))
        user.signIn(ADA)
    }

    @AfterEach
    fun endTheRequest() {
        RequestContextHolder.resetRequestAttributes()
    }

    override fun uploaded(): FileId = uploaded(USAGE)

    private fun uploaded(usage: String): FileId =
        uploads.upload(usage, "photo.jpg", "image/jpeg", JPEG.size.toLong(), ByteArrayResource(JPEG))

    override fun <T> inTransaction(block: () -> T): T = transactions.execute { block() }!!

    @Test
    fun `only its uploader keeps a temporary file`() {
        // Given
        val photo = uploaded()
        user.signIn(GRACE)

        // When & Then
        shouldThrow<FileNotFound> { inTransaction { storage.keep(photo, USAGE, EVENT) } }
    }

    @Test
    fun `keeping a file outside a transaction fails`() {
        // Given
        val photo = uploaded()

        // When & Then
        shouldThrow<IllegalTransactionStateException> { storage.keep(photo, USAGE, EVENT) }
    }

    @Test
    fun `a reader the FileAccess of the file's object allows gets a URL`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, EVENT) }
        user.signIn(GRACE)

        // When & Then
        shouldNotThrowAny { storage.urlOf(photo) }
    }

    @Test
    fun `an anonymous reader gets the stable URL of a file of a public usage`() {
        // Given
        val avatar = uploaded(PUBLIC_USAGE)
        inTransaction { storage.keep(avatar, PUBLIC_USAGE, FileResource("user", ADA)) }
        user.signOut()

        // When
        val url = storage.urlOf(avatar)

        // Then
        url shouldBe URI("/files/public/${avatar.value}")
    }

    @Test
    fun `a file the backend stores outside a request is kept by its caller's transaction`() {
        // Given
        val image = asTheBackend { storage.store(PUBLIC_USAGE, "image.jpg", ByteArrayResource(JPEG)) }

        // When
        asTheBackend { inTransaction { storage.keep(image, PUBLIC_USAGE, FileResource("catalog-bottle", "1")) } }

        // Then
        shouldNotThrowAny { storage.urlOf(image) }
    }

    @Test
    fun `a request without a user cannot keep a file the backend stored`() {
        // Given
        val image = asTheBackend { storage.store(PUBLIC_USAGE, "image.jpg", ByteArrayResource(JPEG)) }
        user.signOut()

        // When & Then
        shouldThrow<FileNotFound> {
            inTransaction { storage.keep(image, PUBLIC_USAGE, FileResource("catalog-bottle", "1")) }
        }
    }

    @Test
    fun `a user the FileAccess of the file's object allows deletes it`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, EVENT) }
        user.signIn(GRACE)

        // When
        inTransaction { storage.delete(photo) }

        // Then
        shouldThrow<FileNotFound> { storage.urlOf(photo) }
    }

    @Test
    fun `the backend deletes any file outside a request`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, EVENT) }

        // When
        asTheBackend { inTransaction { storage.delete(photo) } }

        // Then
        shouldThrow<FileNotFound> { storage.urlOf(photo) }
    }

    @Test
    fun `a write that fails midway leaves no object`() {
        // Given
        val stored = directory.listDirectoryEntries()
        val dropped = object : InputStream() {
            override fun read(): Int = throw IOException("The connection dropped")
        }
        val streams = mutableListOf(JPEG.inputStream(), SequenceInputStream(JPEG.inputStream(), dropped))

        // When
        shouldThrow<IOException> {
            uploads.upload(USAGE, "photo.jpg", "image/jpeg", JPEG.size.toLong()) { streams.removeFirst() }
        }

        // Then
        directory.listDirectoryEntries() shouldContainExactlyInAnyOrder stored
    }

    @Test
    fun `the backend reads any file outside a request`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, FileResource("event", "7")) }

        // When & Then
        asTheBackend { shouldNotThrowAny { storage.urlOf(photo) } }
    }

    @Test
    fun `a user outside a request is not the backend`() {
        // Given
        val photo = uploaded()
        inTransaction { storage.keep(photo, USAGE, FileResource("event", "7")) }
        user.signIn(GRACE)

        // When & Then
        outsideARequest { shouldThrow<FileNotFound> { inTransaction { storage.delete(photo) } } }
    }

    private fun <T> asTheBackend(block: () -> T): T {
        val signedIn = user.orNull()
        user.signOut()

        return try {
            outsideARequest(block)
        } finally {
            signedIn?.let { user.signIn(it.id) }
        }
    }

    private fun <T> outsideARequest(block: () -> T): T {
        val request = RequestContextHolder.getRequestAttributes()
        RequestContextHolder.resetRequestAttributes()

        return try {
            block()
        } finally {
            RequestContextHolder.setRequestAttributes(request)
        }
    }

    companion object {
        private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    }
}
