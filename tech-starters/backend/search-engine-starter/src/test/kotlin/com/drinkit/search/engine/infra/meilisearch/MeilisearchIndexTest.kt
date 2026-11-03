package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.search.engine.Cellar
import com.drinkit.search.engine.MeilisearchIntegrationTest
import com.drinkit.search.engine.MeilisearchServer
import com.drinkit.search.engine.SearchDocumentSource
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.SearchIndexTestContract
import com.drinkit.search.engine.Wine
import com.drinkit.search.engine.generated.jooq.DrinkitApplication
import com.drinkit.search.engine.infra.Instances
import com.drinkit.search.engine.infra.declareIndex
import com.fasterxml.jackson.annotation.JsonProperty
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.SearchRequest
import com.meilisearch.sdk.model.Hybrid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.context.ConfigurableApplicationContext
import java.util.UUID

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@MeilisearchIntegrationTest
internal class MeilisearchIndexTest : SearchIndexTestContract() {

    override fun declareWines(name: String, source: SearchDocumentSource<Wine>, primaryKey: String): SearchIndex<Wine> =
        application.declareIndex(name, source, primaryKey = primaryKey)

    @Test
    fun `refuses an index declared twice`() {
        // Given
        val name = "wines-${UUID.randomUUID()}"
        application.declareIndex(name, Cellar())

        // When, then
        shouldThrow<IllegalArgumentException> { application.declareIndex(name, Cellar()) }
    }

    @Test
    fun `finds by a hybrid search a document that carries its vectors`() {
        // Given
        val wines = application.declareIndex(
            "described-${UUID.randomUUID()}",
            NoDescribedWines,
            settings = "search/described-wines.json",
        )
        wines.save(
            listOf(
                DescribedWine("1", "Margaux", mapOf("description" to listOf(1.0, 0.0, 0.0))),
                DescribedWine("2", "Pomerol", mapOf("description" to listOf(0.0, 1.0, 0.0))),
            ),
        )

        // When
        @Suppress("ArrayPrimitive") // The SDK takes a Double[]
        val hits = client.index(wines.name).search(
            SearchRequest.builder()
                .vector(arrayOf(0.9, 0.1, 0.0))
                .hybrid(Hybrid.builder().embedder("description").semanticRatio(1.0).build())
                .build(),
        ).hits

        // Then
        hits.first()["id"] shouldBe "1"
    }

    internal data class DescribedWine(
        val id: String,
        val name: String,
        @get:JsonProperty("_vectors") val vectors: Map<String, List<Double>>,
    )

    private object NoDescribedWines : SearchDocumentSource<DescribedWine> {
        override fun ids(after: String?, limit: Int): List<String> = emptyList()

        override fun documents(ids: List<String>): List<DescribedWine> = emptyList()
    }

    // One application for the class: each test names its indexes after a random id
    companion object {

        private lateinit var instances: Instances

        private lateinit var application: ConfigurableApplicationContext

        private lateinit var client: Client

        @JvmStatic
        @BeforeAll
        fun start(server: MeilisearchServer, client: Client) {
            instances = Instances(server)
            application = instances.start()
            this.client = client
        }

        @JvmStatic
        @AfterAll
        fun stop() = instances.close()
    }
}
