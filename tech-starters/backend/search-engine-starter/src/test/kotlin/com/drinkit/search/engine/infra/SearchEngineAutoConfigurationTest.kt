package com.drinkit.search.engine.infra

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.search.engine.Cellar
import com.drinkit.search.engine.MeilisearchIntegrationTest
import com.drinkit.search.engine.MeilisearchServer
import com.drinkit.search.engine.SearchEngine
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.SearchIndexes
import com.drinkit.search.engine.Wine
import com.drinkit.search.engine.declare
import com.drinkit.search.engine.generated.jooq.DrinkitApplication
import com.drinkit.search.engine.infra.meilisearch.MeilisearchEngine
import com.drinkit.search.engine.infra.meilisearch.MeilisearchIndexes
import com.meilisearch.sdk.Client
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.env.Environment
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@MeilisearchIntegrationTest
internal class SearchEngineAutoConfigurationTest {

    // The auto-configurations come from the AutoConfiguration.imports files, as in an application
    private fun runner(server: MeilisearchServer) = ApplicationContextRunner()
        .withUserConfiguration(SearchEngineApplication::class.java)
        .withPropertyValues(*SearchEngineApplication.properties(server))

    @Test
    fun `backs off before a client of the application`(server: MeilisearchServer, client: Client) {
        // Given
        val withOwnClient = runner(server).withBean(Client::class.java, { client })

        withOwnClient.run { context ->
            // When & Then
            context.getBean<Client>() shouldBeSameInstanceAs client
        }
    }

    @Test
    fun `provides the indexes and the engine of Meilisearch`(server: MeilisearchServer) {
        runner(server).run { context ->
            // When & Then
            context.getBean<SearchIndexes>().shouldBeInstanceOf<MeilisearchIndexes>()
            context.getBean<SearchEngine>().shouldBeInstanceOf<MeilisearchEngine>()
        }
    }

    @Test
    fun `is switched off by its enabled property`(server: MeilisearchServer) {
        // Given
        val disabled = runner(server).withPropertyValues("starters.search-engine.enabled=false")

        disabled.run { context ->
            // When & Then
            context.getBeansOfType(SearchIndexes::class.java).values.shouldBeEmpty()
        }
    }

    @Test
    fun `registers nothing without a jOOQ DSLContext`(server: MeilisearchServer) {
        // Given
        val withoutJooq =
            runner(server).withPropertyValues("spring.autoconfigure.exclude=${JooqAutoConfiguration::class.java.name}")

        withoutJooq.run { context ->
            // When & Then
            context.shouldStart()
            context.getBeansOfType(SearchIndexes::class.java).values.shouldBeEmpty()
        }
    }

    @Test
    fun `reports the engine up when it answers`(server: MeilisearchServer) {
        runner(server).run { context ->
            // When & Then
            context.getBean<HealthIndicator>("meilisearchHealthIndicator").health()?.status shouldBe Status.UP
        }
    }

    @Test
    fun `reports the engine down when it does not answer`(server: MeilisearchServer) {
        // Given
        val unreachable = runner(server).withPropertyValues("starters.search-engine.meilisearch.url=http://localhost:1")

        unreachable.run { context ->
            // When & Then
            context.getBean<HealthIndicator>("meilisearchHealthIndicator").health()?.status shouldBe Status.DOWN
        }
    }

    @Test
    fun `reports no health without the health module of Spring Boot`(server: MeilisearchServer) {
        // Given
        val withoutHealth = runner(server).withClassLoader(FilteredClassLoader(HealthIndicator::class.java))

        withoutHealth.run { context ->
            // When & Then
            context.shouldStart()
            context.containsBean("meilisearchHealthIndicator") shouldBe false
        }
    }

    @Test
    fun `creates a declared index at startup with the settings of its file`(server: MeilisearchServer, client: Client) {
        // Given
        val name = "wines-${UUID.randomUUID()}"
        val withWines = runner(server).withIndex(name, "search/wines.json")

        withWines.run { context ->
            // When & Then
            context.shouldStart()
            eventually { client.getIndex(name).settings.searchableAttributes shouldBe arrayOf("name", "grapes") }
        }
    }

    @Test
    fun `applies the settings file of an index again at the next startup`(server: MeilisearchServer, client: Client) {
        // Given
        val name = "wines-${UUID.randomUUID()}"
        runner(server).withIndex(name, "search/wines.json").run { it.shouldStart() }

        // When
        runner(server).withIndex(name, "search/wines-by-name.json").run { it.shouldStart() }

        // Then
        eventually { client.getIndex(name).settings.searchableAttributes shouldBe arrayOf("name") }
    }

    @Test
    fun `starts even when the engine does not answer`(server: MeilisearchServer) {
        // Given
        val unreachable = runner(server).withPropertyValues("starters.search-engine.meilisearch.url=http://localhost:1")
            .withIndex("wines", "search/wines.json")

        unreachable.run { context ->
            // When & Then
            context.shouldStart()
        }
    }

    @Test
    fun `refuses to start on an index declared without its settings file`(server: MeilisearchServer) {
        // Given
        val withMissingSettings = runner(server).withIndex("wines", "search/missing.json")

        withMissingSettings.run { context ->
            // When & Then
            val failure = context.startupFailure.shouldNotBeNull()
            NestedExceptionUtils.getMostSpecificCause(failure).message shouldBe
                "No settings file search/missing.json on the classpath"
        }
    }

    private fun ApplicationContextRunner.withIndex(name: String, settings: String) =
        withUserConfiguration(WinesConfiguration::class.java)
            .withPropertyValues("test.wines.name=$name", "test.wines.settings=$settings")

    @Configuration(proxyBeanMethods = false)
    class WinesConfiguration {

        @Bean
        fun wines(indexes: SearchIndexes, environment: Environment): SearchIndex<Wine> = indexes.declare(
            environment.getRequiredProperty("test.wines.name"),
            Cellar(),
            settings = environment.getRequiredProperty("test.wines.settings"),
        )
    }

    private fun AssertableApplicationContext.shouldStart() = startupFailure.shouldBeNull()

    // The settings go out at startup without waiting for the engine, which applies them a moment later
    private fun eventually(assertion: () -> Unit) = runBlocking { eventually(10.seconds) { assertion() } }
}
