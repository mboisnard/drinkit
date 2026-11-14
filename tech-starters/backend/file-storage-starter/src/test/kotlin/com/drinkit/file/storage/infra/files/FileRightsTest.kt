package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileResource
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.ADA
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.ADMIN
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.GRACE
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.PRIVATE_USAGE
import com.drinkit.file.storage.infra.files.FileStorageTestContext.Companion.PUBLIC_USAGE
import com.drinkit.file.storage.infra.files.FileStorageTestContext.EventMembers
import com.drinkit.security.ConnectedUser
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class FileRightsTest {

    private val members = EventMembers()
    private val rights = FileRights(setOf(PUBLIC_USAGE), setOf("ROLE_ADMIN"), listOf(members))

    @Test
    fun `a reader the FileAccess of the file's object refuses cannot read it`() {
        // Given
        val photo = keptOn(FileResource("event", "7"))

        // When & Then
        rights.readable(listOf(photo), GRACE_USER).shouldBeEmpty()
    }

    @Test
    fun `a reader the FileAccess of the file's object allows reads it`() {
        // Given
        val photo = keptOn(EVENT_42)

        // When & Then
        rights.readable(listOf(photo), GRACE_USER) shouldBe listOf(photo)
    }

    @Test
    fun `a reader with a read-all role reads any file`() {
        // Given
        val photo = keptOn(FileResource("event", "7"))

        // When & Then
        rights.readable(listOf(photo), ConnectedUser(ADMIN, setOf("ROLE_USER", "ROLE_ADMIN"))) shouldBe listOf(photo)
    }

    @Test
    fun `a file kept on an object no FileAccess covers is read by its uploader only`() {
        // Given
        val photo = keptOn(FileResource("cellar", "1"))

        // When & Then
        rights.readable(listOf(photo), GRACE_USER).shouldBeEmpty()
        rights.readable(listOf(photo), ADA_USER) shouldBe listOf(photo)
    }

    @Test
    fun `an anonymous reader reads a file of a public usage`() {
        // Given
        val avatar = keptOn(FileResource("user", ADA), PUBLIC_USAGE)

        // When & Then
        rights.readable(listOf(avatar), null) shouldBe listOf(avatar)
    }

    @Test
    fun `a list asks the FileAccess of an object once for all its files`() {
        // Given
        val photos = listOf(keptOn(EVENT_42), keptOn(EVENT_42))

        // When
        val readable = rights.readable(photos, GRACE_USER)

        // Then
        readable shouldBe photos
        members.questions shouldBe 1
    }

    @Test
    fun `its uploader, or whom the FileAccess of its object allows, deletes a file`() {
        // Given
        val photo = keptOn(EVENT_42)
        val other = keptOn(FileResource("event", "7"))

        // When & Then
        rights.canDelete(photo, GRACE_USER) shouldBe true
        rights.canDelete(other, GRACE_USER) shouldBe false
        rights.canDelete(other, ADA_USER) shouldBe true
    }

    private fun keptOn(resource: FileResource, usage: String = PRIVATE_USAGE) = StoredFile(
        id = FileId(UUID.randomUUID().toString()),
        usage = usage,
        ownerId = ADA,
        type = "image/jpeg",
        size = 4,
        name = "photo.jpg",
        uploadedAt = Instant.EPOCH,
        resource = resource,
    )

    private companion object {
        val ADA_USER = ConnectedUser(ADA, setOf("ROLE_USER"))
        val GRACE_USER = ConnectedUser(GRACE, setOf("ROLE_USER"))
        val EVENT_42 = FileResource("event", "42")
    }
}
