package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.search.engine.Cellar
import com.drinkit.search.engine.MeilisearchIntegrationTest
import com.drinkit.search.engine.MeilisearchServer
import com.drinkit.search.engine.RebuildProgress
import com.drinkit.search.engine.SearchEngine
import com.drinkit.search.engine.SearchEngineTestContract
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.Wine
import com.drinkit.search.engine.generated.jooq.DrinkitApplication
import com.drinkit.search.engine.infra.Instances
import com.drinkit.search.engine.infra.LostEvents
import com.drinkit.search.engine.infra.declareIndex
import io.kotest.matchers.longs.shouldBePositive
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import java.util.UUID

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@MeilisearchIntegrationTest
internal class MeilisearchEngineTest : SearchEngineTestContract() {

    private lateinit var instances: Instances

    private val application by lazy { instances.start() }

    @BeforeEach
    fun setup(server: MeilisearchServer) {
        instances = Instances(server)
    }

    @AfterEach
    fun stop() = instances.close()

    override fun declareWines(name: String): SearchIndex<Wine> = application.declareIndex(name, Cellar())

    override fun engine(): SearchEngine = application.getBean<SearchEngine>()

    @Test
    fun `gives the version of the engine`() {
        engine().information().version.shouldNotBeBlank()
    }

    @Test
    fun `gives the size the engine takes`() {
        engine().information().size.shouldBePositive()
    }

    @Test
    fun `lists the next index of a running rebuild apart from the index it rebuilds`() {
        // Given
        val instance = instances.start(LostEvents::class.java)
        val wines = instance.declareIndex("wines-${UUID.randomUUID()}", Cellar(Wine("1", "Margaux")))
        wines.rebuild()

        // When
        val indexes = instance.getBean<SearchEngine>().information().indexes

        // Then
        indexes.single { it.name == wines.name }.rebuild shouldBe RebuildProgress(doneBatches = 0, totalBatches = 1)
        indexes.single { it.name == "${wines.name}_next" }.declared shouldBe false
    }
}
