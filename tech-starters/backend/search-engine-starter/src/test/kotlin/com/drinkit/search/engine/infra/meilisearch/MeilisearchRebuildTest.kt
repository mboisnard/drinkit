package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.search.engine.Cellar
import com.drinkit.search.engine.MeilisearchIntegrationTest
import com.drinkit.search.engine.MeilisearchServer
import com.drinkit.search.engine.RebuildProgress
import com.drinkit.search.engine.SearchDocumentSource
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.SearchIndexes
import com.drinkit.search.engine.Wine
import com.drinkit.search.engine.awaitRebuilt
import com.drinkit.search.engine.generated.jooq.DrinkitApplication
import com.drinkit.search.engine.infra.Instances
import com.drinkit.search.engine.infra.LostEvents
import com.drinkit.search.engine.infra.declareIndex
import com.drinkit.search.engine.infra.rebuild.SearchIndexBatchRequested
import com.drinkit.test.ControlledClock
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.model.TasksQuery
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import java.time.Duration
import java.util.UUID

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@MeilisearchIntegrationTest
internal class MeilisearchRebuildTest {

    private val name = "wines-${UUID.randomUUID()}"

    private val cellar = Cellar(Wine("1", "Margaux"), Wine("2", "Pomerol"))

    private lateinit var instances: Instances

    private lateinit var client: Client

    @BeforeEach
    fun setup(server: MeilisearchServer, client: Client) {
        instances = Instances(server)
        this.client = client
    }

    @AfterEach
    fun stop() = instances.close()

    @Test
    fun `ends a rebuild whose batches are loaded by different instances`() {
        // Given
        val first = losingEvents(batchSize = 1).also { it.wines().rebuild() }
        val other = losingEvents(batchSize = 1)
        val wines = other.wines()

        // When
        first.deliver(first.batch(0))
        other.deliver(first.batch(1))

        // Then
        listOf("1", "2").map(wines::find) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"))
    }

    @Test
    fun `gives the progress of the running rebuild when asked to rebuild again`() {
        // Given
        val wines = losingEvents().wines().also { it.rebuild() }

        // When
        val progress = wines.rebuild()

        // Then
        progress shouldBe RebuildProgress(doneBatches = 0, totalBatches = 1)
    }

    @Test
    fun `publishes no batch for a rebuild running for less than restart-after`() {
        // Given
        losingEvents().wines().rebuild()
        val other = losingEvents().later(Duration.ofMinutes(29))

        // When
        other.wines().rebuild()

        // Then
        other.getBean<SpyPlatformEventPublisher>().eventsOfType(SearchIndexBatchRequested::class).shouldBeEmpty()
    }

    @Test
    fun `restarts from scratch a rebuild running for longer than restart-after`() {
        // Given
        losingEvents().wines().rebuild()
        val other = instances.start().later(Duration.ofMinutes(31)).wines()

        // When
        other.rebuild()
        other.awaitRebuilt()

        // Then
        listOf("1", "2").map(other::find) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"))
    }

    @Test
    fun `restarts from scratch a rebuild whose swap went out before its instance stopped`() {
        // Given
        val wines = losingEvents().wines() as MeilisearchIndex<Wine>
        wines.save(Wine("9", "Gone"))
        wines.rebuild()
        wines.next.save(cellar.documents(listOf("1", "2")))
        wines.next.await(wines.next.swapWith(wines.live))
        val other = instances.start().later(Duration.ofMinutes(31)).wines()

        // When
        other.rebuild()
        other.awaitRebuilt()

        // Then
        listOf("1", "2", "9").map(other::find) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"), null)
    }

    @Test
    fun `restarts from scratch a rebuild whose preparation stopped`() {
        // Given
        val unreachable = instances.start(
            properties = listOf("starters.search-engine.meilisearch.url=http://localhost:1"),
        )
        runCatching { unreachable.wines().rebuild() }
        val other = instances.start().later(Duration.ofMinutes(31)).wines()

        // When
        other.rebuild()
        other.awaitRebuilt()

        // Then
        listOf("1", "2").map(other::find) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"))
    }

    @Test
    fun `counts a batch delivered twice once`() {
        // Given
        val instance = losingEvents(batchSize = 1)
        val wines = instance.wines().also { it.rebuild() }

        // When
        repeat(2) { instance.deliver(instance.batch(0)) }

        // Then
        wines.rebuildProgress() shouldBe RebuildProgress(doneBatches = 1, totalBatches = 2)
    }

    @Test
    fun `ignores a batch of a previous rebuild`() {
        // Given
        val first = losingEvents(batchSize = 1).also { it.wines().rebuild() }
        val other = losingEvents(batchSize = 1).later(Duration.ofMinutes(31))
        val wines = other.wines().also { it.rebuild() }

        // When
        other.deliver(first.batch(0))

        // Then
        wines.rebuildProgress() shouldBe RebuildProgress(doneBatches = 0, totalBatches = 2)
    }

    @Test
    fun `leaves no next index for a batch delivered again once swapped`() {
        // Given
        val instance = losingEvents()
        instance.wines().rebuild()
        instance.deliver(instance.batch(0))

        // When
        instance.deliver(instance.batch(0))

        // Then
        client.stats.indexes.keys shouldNotContain "${name}_next"
    }

    @Test
    fun `loads nothing from a batch of a previous rebuild`() {
        // Given
        val first = losingEvents(batchSize = 1).also { it.wines().rebuild() }
        val other = losingEvents(batchSize = 1).later(Duration.ofMinutes(31))
        other.wines().rebuild()

        // When
        other.deliver(first.batch(0))

        // Then
        client.index("${name}_next").stats.numberOfDocuments shouldBe 0
    }

    @Test
    fun `swaps the index not before its last batch is done`() {
        // Given
        val instance = losingEvents(batchSize = 1)
        instance.wines().rebuild()

        // When
        instance.deliver(instance.batch(0))

        // Then
        swaps() shouldBe 0
    }

    @Test
    fun `swaps the index once its last batch is done`() {
        // Given
        val instance = losingEvents(batchSize = 1)
        instance.wines().rebuild()
        instance.deliver(instance.batch(0))

        // When
        instance.deliver(instance.batch(1))

        // Then
        swaps() shouldBe 1
    }

    @Test
    fun `keeps a document saved during the rebuild once it swaps`() {
        // Given
        val instance = losingEvents()
        val wines = instance.wines().also { it.rebuild() }
        Wine("3", "Pauillac").let {
            cellar.store(it)
            wines.save(it)
        }

        // When
        instance.deliver(instance.batch(0))

        // Then
        wines.find("3") shouldBe Wine("3", "Pauillac")
    }

    @Test
    fun `keeps out a document deleted after its batch was loaded once it swaps`() {
        // Given
        val instance = losingEvents(batchSize = 1)
        val wines = instance.wines().also { it.rebuild() }
        instance.deliver(instance.batch(0))
        cellar.remove("1")
        wines.delete("1")

        // When
        instance.deliver(instance.batch(1))

        // Then
        wines.find("1").shouldBeNull()
    }

    @Test
    fun `keeps out a document deleted while its batch was read once it swaps`() {
        // Given
        val instance = losingEvents()
        lateinit var wines: SearchIndex<Wine>
        val deletedOnRead = object : SearchDocumentSource<Wine> by cellar {
            override fun documents(ids: List<String>): List<Wine> = cellar.documents(ids).also {
                if (cellar.documents(listOf("1")).isNotEmpty()) {
                    cellar.remove("1")
                    wines.delete("1")
                }
            }
        }
        wines = instance.declareIndex(name, deletedOnRead)
        wines.rebuild()

        // When
        instance.deliver(instance.batch(0))

        // Then
        wines.find("1").shouldBeNull()
    }

    @Test
    fun `deletes a document while its rebuild drops its next index`() {
        // Given
        val wines = losingEvents().wines()
        wines.save(Wine("1", "Margaux"))
        wines.rebuild()
        MeilisearchTasks(client, Duration.ofSeconds(30)).await(name, client.deleteIndex("${name}_next").taskUid)

        // When
        wines.delete("1")

        // Then
        wines.find("1").shouldBeNull()
    }

    @Test
    fun `clears a setting removed from the settings file once rebuilt`() {
        // Given
        instances.start().wines("search/wines.json")
        val wines = instances.start().wines("search/wines-by-name.json")

        // When
        wines.rebuild()
        wines.awaitRebuilt()

        // Then
        val settings = client.getIndex(name).settings
        settings.searchableAttributes shouldBe arrayOf("name")
        settings.filterableAttributesConfig.flatMap { it.attributePatterns.toList() } shouldBe listOf("id")
    }

    @Test
    fun `keeps the settings of a start made during the rebuild once it swaps`() {
        // Given
        val first = losingEvents()
        first.wines("search/wines.json").rebuild()
        losingEvents().wines("search/wines-by-name.json")

        // When
        first.deliver(first.batch(0))

        // Then
        client.getIndex(name).settings.searchableAttributes shouldBe arrayOf("name")
    }

    @Test
    fun `rebuilds over the next index an interrupted rebuild left`() {
        // Given
        val tasks = MeilisearchTasks(client, Duration.ofSeconds(30))
        tasks.await(name, client.createIndex("${name}_next", "id").taskUid)
        tasks.await(name, client.index("${name}_next").addDocuments("""[{"id": "9", "name": "Left"}]""", "id").taskUid)
        val wines = instances.start().wines()

        // When
        wines.rebuild()
        wines.awaitRebuilt()

        // Then
        listOf("1", "9").map(wines::find) shouldBe listOf(Wine("1", "Margaux"), null)
    }

    @Test
    fun `rebuilds an index the engine lost`() {
        // Given
        val wines = instances.start().wines()
        MeilisearchTasks(client, Duration.ofSeconds(30)).await(name, client.deleteIndex(name).taskUid)

        // When
        wines.rebuild()
        wines.awaitRebuilt()

        // Then
        listOf("1", "2").map(wines::find) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"))
    }

    private fun losingEvents(batchSize: Int = 1000) = instances.start(
        LostEvents::class.java,
        properties = listOf("starters.search-engine.rebuild.batch-size=$batchSize"),
    )

    private fun ConfigurableApplicationContext.wines(settings: String = "search/wines.json"): SearchIndex<Wine> =
        declareIndex(name, cellar, settings)

    private fun ConfigurableApplicationContext.later(duration: Duration) = apply {
        getBean<ControlledClock>().add(duration)
    }

    private fun ConfigurableApplicationContext.batch(number: Long) = getBean<SpyPlatformEventPublisher>()
        .eventsOfType(SearchIndexBatchRequested::class)
        .single { it.batch == number }

    private fun ConfigurableApplicationContext.deliver(batch: SearchIndexBatchRequested) =
        (getBean<SearchIndexes>() as MeilisearchIndexes).onBatchRequested(batch)

    private fun swaps() = client.getTasks(TasksQuery().setTypes(arrayOf("indexSwap"))).results
        .count { task -> task.details?.swaps.orEmpty().any { name in it.indexes } }
}
