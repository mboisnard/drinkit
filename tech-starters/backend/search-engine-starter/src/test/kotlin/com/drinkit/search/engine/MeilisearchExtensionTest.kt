package com.drinkit.search.engine

import com.meilisearch.sdk.Client
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MeilisearchExtension::class)
internal class MeilisearchExtensionTest {

    @Test
    fun `should provide a client connected to the container`(client: Client) {
        client.createIndex("cellars")

        client.indexes.results.map { it.uid } shouldBe listOf("cellars")
    }
}
