package com.drinkit.search.engine.infra

import com.drinkit.search.engine.MeilisearchServer
import com.drinkit.search.engine.SearchDocumentSource
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.SearchIndexes
import com.drinkit.search.engine.declare
import com.drinkit.test.ControlledClock
import org.springframework.beans.factory.getBean
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

@SpringBootConfiguration
@EnableAutoConfiguration
internal class SearchEngineApplication {

    @Bean
    fun clock(): ControlledClock = ControlledClock().fix()

    @Bean
    fun jsonMapper(): JsonMapper = jacksonMapperBuilder().build()

    companion object {
        fun properties(server: MeilisearchServer) = arrayOf(
            "starters.search-engine.meilisearch.url=${server.url}",
            "starters.search-engine.meilisearch.api-key=${server.apiKey}",
        )
    }
}

// The applications a test starts, each one an instance: two of them on the same database and engine play two pods
internal class Instances(private val server: MeilisearchServer) : AutoCloseable {

    private val started = mutableListOf<ConfigurableApplicationContext>()

    fun start(vararg sources: Class<*>, properties: List<String> = emptyList()): ConfigurableApplicationContext =
        SpringApplicationBuilder(SearchEngineApplication::class.java, *sources)
            .web(WebApplicationType.NONE)
            .properties(*SearchEngineApplication.properties(server), *properties.toTypedArray())
            .run()
            .also { started += it }

    override fun close() = started.forEach { it.close() }
}

internal inline fun <reified T : Any> ConfigurableApplicationContext.declareIndex(
    name: String,
    source: SearchDocumentSource<T>,
    settings: String = "search/wines.json",
    primaryKey: String = "id",
): SearchIndex<T> = getBean<SearchIndexes>().declare(name, source, primaryKey, settings)
