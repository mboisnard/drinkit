package com.drinkit.search.engine

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MeilisearchConfig(
    @Value("meilisearch.url")
    private val url: String,
    @Value("meilisearch.apiKey")
    private val apiKey: String,
) {

    @Bean
    fun meilisearchClient(): Client {
        val config = Config(url, apiKey)
        return Client(config)
    }
}