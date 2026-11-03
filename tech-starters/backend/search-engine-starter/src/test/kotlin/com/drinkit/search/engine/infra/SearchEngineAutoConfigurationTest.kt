package com.drinkit.search.engine.infra

import com.drinkit.search.engine.MeilisearchIntegrationTest
import com.drinkit.search.engine.MeilisearchServer
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@MeilisearchIntegrationTest
internal class SearchEngineAutoConfigurationTest {

    private fun runner(server: MeilisearchServer) = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                SearchEngineAutoConfiguration::class.java,
                SearchEngineHealthAutoConfiguration::class.java,
            ),
        )
        .withPropertyValues(
            "starters.search-engine.meilisearch.url=${server.url}",
            "starters.search-engine.meilisearch.api-key=${server.apiKey}",
        )

    @Test
    fun `connects a Meilisearch client to the engine of its properties`(server: MeilisearchServer) {
        runner(server).run { context ->
            context.getBean<Client>().isHealthy shouldBe true
        }
    }

    @Test
    fun `backs off before a client of the application`(server: MeilisearchServer) {
        val client = Client(Config(server.url, server.apiKey))

        runner(server).withBean(Client::class.java, { client })
            .run { context ->
                context.getBean<Client>() shouldBeSameInstanceAs client
            }
    }

    @Test
    fun `is switched off by its enabled property`(server: MeilisearchServer) {
        runner(server).withPropertyValues("starters.search-engine.enabled=false")
            .run { context ->
                context.getBeansOfType(Client::class.java).values.shouldBeEmpty()
            }
    }

    @Test
    fun `reports the engine up when it answers`(server: MeilisearchServer) {
        runner(server).run { context ->
            context.getBean<HealthIndicator>("meilisearchHealthIndicator").health()?.status shouldBe Status.UP
        }
    }

    @Test
    fun `reports the engine down when it does not answer`(server: MeilisearchServer) {
        runner(server).withPropertyValues("starters.search-engine.meilisearch.url=http://localhost:1")
            .run { context ->
                context.getBean<HealthIndicator>("meilisearchHealthIndicator").health()?.status shouldBe Status.DOWN
            }
    }

    @Test
    fun `reports no health without the health module of Spring Boot`(server: MeilisearchServer) {
        runner(server).withClassLoader(FilteredClassLoader(HealthIndicator::class.java))
            .run { context ->
                context.startupFailure.shouldBeNull()
                context.containsBean("meilisearchHealthIndicator") shouldBe false
            }
    }
}
