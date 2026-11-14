package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileResource
import com.drinkit.file.storage.generated.jooq.DrinkitApplication
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.ADA
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.PRIVATE_USAGE
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.PUBLIC_USAGE
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.InMemoryCurrentUser
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.servlet.function.RouterFunctions

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@SpringBootTest(classes = [FileStorageTestContext::class])
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class PublicFileApiTest(
    private val files: StoredFiles,
    private val uploads: FileUploads,
    storage: Storage,
    private val transactions: TransactionTemplate,
    private val user: InMemoryCurrentUser,
) {

    private val mockMvc: MockMvc = MockMvcBuilders.routerFunctions(
        RouterFunctions.route().GET(PUBLIC_FILE_PATH, PublicFileApi(files, storage)::serve).build(),
    ).build()

    @Test
    fun `a kept file of a public usage is served to anyone, cacheable`() {
        // Given
        val avatar = kept(PUBLIC_USAGE)
        user.signOut()

        // When
        val response = mockMvc.get("/files/public/${avatar.value}")

        // Then
        response.andExpect {
            status { isOk() }
            content { bytes(JPEG) }
            header { string("Cache-Control", "public, max-age=86400") }
            header { string("X-Content-Type-Options", "nosniff") }
        }
    }

    @Test
    fun `a kept file of a private usage is not found`() {
        // Given
        val photo = kept(PRIVATE_USAGE)

        // When
        val response = mockMvc.get("/files/public/${photo.value}")

        // Then
        response.andExpect { status { isNotFound() } }
    }

    @Test
    fun `a deleted file of a public usage is not found`() {
        // Given
        val avatar = kept(PUBLIC_USAGE)
        transactions.executeWithoutResult { files.delete(avatar) }

        // When
        val response = mockMvc.get("/files/public/${avatar.value}")

        // Then
        response.andExpect { status { isNotFound() } }
    }

    private fun kept(usage: String): FileId {
        user.signIn(ADA)
        val id = uploads.upload(usage, "photo.jpg", "image/jpeg", JPEG.size.toLong(), ByteArrayResource(JPEG))
        transactions.executeWithoutResult { files.keep(id, usage, FileResource("user", ADA)) }

        return id
    }

    companion object {
        private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    }
}
