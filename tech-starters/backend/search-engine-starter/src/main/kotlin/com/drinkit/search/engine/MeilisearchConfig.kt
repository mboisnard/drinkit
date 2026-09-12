package com.drinkit.search.engine

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import com.meilisearch.sdk.json.GsonJsonHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class MeilisearchConfig(
    @Value("\${meilisearch.url}")
    private val url: String,
    @Value("\${meilisearch.apiKey}")
    private val apiKey: String,
) {

    @Bean
    fun meilisearchClient(): Client {
        // Gson is the SDK default and what MeilisearchExtension already uses in tests
        val config = Config(url, apiKey, GsonJsonHandler())
        return Client(config)
    }
}
