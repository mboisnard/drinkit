package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.generated.jooq.DrinkitApplication
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.ADA
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.inMemoryRateLimiter
import com.drinkit.security.InMemoryCurrentUser
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockPart
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.function.RouterFunctions
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.listDirectoryEntries

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@SpringBootTest(classes = [FileStorageTestContext::class])
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class FileUploadApiTest(
    uploads: FileUploads,
    cleanUp: FileCleanUp,
    user: InMemoryCurrentUser,
    private val directory: Path,
) {

    private val onePerTenMinutes = RateLimitRule("file-upload", listOf(RateLimit(1, Duration.ofMinutes(10))))
    private val api = FileUploadApi(uploads, cleanUp, inMemoryRateLimiter(), onePerTenMinutes, user)

    private val mockMvc: MockMvc =
        MockMvcBuilders.routerFunctions(RouterFunctions.route().POST(UPLOAD_PATH, api::upload).build()).build()

    init {
        user.signIn(ADA)
    }

    @Test
    fun `an accepted upload answers 201 with the id of the stored file`() {
        // Given
        val stored = directory.listDirectoryEntries()

        // When
        val response = upload("photo.jpg", JPEG)

        // Then
        response.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isString() }
        }
        directory.listDirectoryEntries() shouldHaveSize stored.size + 1
    }

    @Test
    fun `a refused upload answers 415 and stores nothing`() {
        // Given
        val stored = directory.listDirectoryEntries()

        // When
        val response = upload("label.jpg", "%PDF-1.7\n".toByteArray())

        // Then
        response.andExpect { status { isUnsupportedMediaType() } }
        directory.listDirectoryEntries() shouldContainExactlyInAnyOrder stored
    }

    @Test
    fun `an upload past the limit of its user answers 429`() {
        // Given
        upload("photo.jpg", JPEG)

        // When
        val response = upload("photo.jpg", JPEG)

        // Then
        response.andExpect { status { isTooManyRequests() } }
    }

    @Test
    fun `an upload without its usage answers 400 and stores nothing`() {
        // Given
        val stored = directory.listDirectoryEntries()

        // When
        val response = mockMvc.multipart(UPLOAD_PATH) { part(MockPart("file", "photo.jpg", JPEG)) }

        // Then
        response.andExpect { status { isBadRequest() } }
        directory.listDirectoryEntries() shouldContainExactlyInAnyOrder stored
    }

    private fun upload(name: String, bytes: ByteArray) = mockMvc.multipart(UPLOAD_PATH) {
        part(MockPart("file", name, bytes), MockPart("usage", "bottle-photo".toByteArray()))
    }

    companion object {
        private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    }
}
