package com.drinkit.search.engine.infra

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.exceptions.MeilisearchException
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean

// An auto-configuration of its own, since starters.no-stereotype refuses a nested @Configuration to guard the bean
@AutoConfiguration(after = [SearchEngineAutoConfiguration::class])
@ConditionalOnClass(HealthIndicator::class)
@ConditionalOnProperty(prefix = "starters.search-engine", name = ["enabled"], matchIfMissing = true)
internal class SearchEngineHealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["meilisearchHealthIndicator"])
    fun meilisearchHealthIndicator(client: Client) = HealthIndicator {
        try {
            client.health()
            Health.up().build()
        } catch (exception: MeilisearchException) {
            Health.down(exception).build()
        }
    }
}
