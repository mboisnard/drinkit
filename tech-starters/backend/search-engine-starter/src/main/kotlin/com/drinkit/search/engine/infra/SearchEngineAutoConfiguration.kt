package com.drinkit.search.engine.infra

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.search-engine", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SearchEngineProperties::class)
internal class SearchEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun meilisearchClient(properties: SearchEngineProperties): Client =
        Client(Config(properties.meilisearch.url, properties.meilisearch.apiKey))
}
