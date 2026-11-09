package com.drinkit.search.engine

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.model.IndexesQuery
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Proxy
import java.util.UUID

private const val MORE_THAN_A_PAGE_OF_INDEXES = 45

@MeilisearchIntegrationTest
internal class MeilisearchExtensionTest {

    // The extension reads nothing from its context
    private val context = Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(ExtensionContext::class.java),
    ) { _, _, _ ->
        error("unused")
    } as ExtensionContext

    @Test
    fun `leaves no index behind once a test class ends, however many it created`(client: Client) {
        // Given
        val extension = MeilisearchExtension().apply { beforeAll(context) }
        val created = List(MORE_THAN_A_PAGE_OF_INDEXES) { "class-${UUID.randomUUID()}" }
        created.map { client.createIndex(it).taskUid }.forEach(client::waitForTask)

        // When
        extension.afterAll(context)

        // Then
        val left = client.getIndexes(IndexesQuery().setLimit(1_000)).results.map { it.uid }
        left.filter { it in created }.shouldBeEmpty()
    }
}
