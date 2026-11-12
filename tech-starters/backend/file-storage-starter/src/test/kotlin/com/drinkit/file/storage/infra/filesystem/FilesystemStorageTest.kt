package com.drinkit.file.storage.infra.filesystem

import com.drinkit.file.storage.infra.files.StorageTestContract
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.function.RouterFunctions
import java.net.URI
import java.nio.file.Files
import java.time.Clock

internal class FilesystemStorageTest : StorageTestContract() {

    override val storage =
        FilesystemStorage(Files.createTempDirectory("files"), "signing-key".toByteArray(), Clock.systemUTC())

    private val mockMvc: MockMvc = MockMvcBuilders.routerFunctions(
        RouterFunctions.route().GET(SIGNED_FILE_PATH, SignedFileApi(storage, TTL)::serve).build(),
    ).build()

    override fun readBack(url: URI): ByteArray? {
        val response = mockMvc.get(url).andReturn().response

        return response.contentAsByteArray.takeIf { response.status == 200 }
    }
}
