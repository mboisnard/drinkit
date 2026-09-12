package com.drinkit.search.engine

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import com.meilisearch.sdk.json.GsonJsonHandler
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "meilisearch")
internal data class MeilisearchProperties(
    val url: String,
    val apiKey: String,
)

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MeilisearchProperties::class)
internal class MeilisearchConfig {

    @Bean
    fun meilisearchClient(properties: MeilisearchProperties): Client {
        // Gson is the SDK default and what MeilisearchExtension already uses in tests
        val config = Config(properties.url, properties.apiKey, GsonJsonHandler())
        return Client(config)
    }
}
