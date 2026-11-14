package com.drinkit.file.storage.infra.filesystem

import com.drinkit.file.storage.infra.files.StorageTestContract
import com.drinkit.test.ControlledClock
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.function.RouterFunctions
import java.net.URI
import java.nio.file.Files
import java.time.Instant

internal class FilesystemStorageTest : StorageTestContract() {

    private val clock = ControlledClock().fix(Instant.parse("2026-11-13T10:00:00Z"))

    override val storage = FilesystemStorage(Files.createTempDirectory("files"), "signing-key".toByteArray(), clock)

    private val mockMvc: MockMvc = MockMvcBuilders.routerFunctions(
        RouterFunctions.route().GET(SIGNED_FILE_PATH, SignedFileApi(storage, TTL)::serve).build(),
    ).build()

    override fun readBack(url: URI): ByteArray? {
        val response = mockMvc.get(url).andReturn().response

        return response.contentAsByteArray.takeIf { response.status == 200 }
    }

    @Test
    fun `a URL answers 403 once expired`() {
        // Given
        val photo = aPhoto()
        storage.put(photo, content())
        val url = storage.urlOf(photo, TTL)

        // When
        clock.add(TTL.plusSeconds(1))

        // Then
        mockMvc.get(url).andExpect { status { isForbidden() } }
    }

    @Test
    fun `a URL whose file name was changed answers 403`() {
        // Given
        val photo = aPhoto()
        storage.put(photo, content())
        val url = storage.urlOf(photo, TTL).toString()

        // When
        val changed = URI.create(url.replace("name=photo.jpg", "name=photo.html"))

        // Then
        mockMvc.get(changed).andExpect { status { isForbidden() } }
    }

    @Test
    fun `a file is served with headers that stop a browser from running it`() {
        // Given
        val photo = aPhoto()
        storage.put(photo, content())

        // When
        val response = mockMvc.get(storage.urlOf(photo, TTL))

        // Then
        response.andExpect {
            status { isOk() }
            content { contentType("image/jpeg") }
            header { string("X-Content-Type-Options", "nosniff") }
            header { string("Content-Security-Policy", "default-src 'none'; sandbox") }
            header { string("Content-Disposition", "inline; filename=\"photo.jpg\"; filename*=UTF-8''photo.jpg") }
        }
    }
}
