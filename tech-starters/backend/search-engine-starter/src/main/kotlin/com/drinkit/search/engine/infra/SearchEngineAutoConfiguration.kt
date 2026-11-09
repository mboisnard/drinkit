package com.drinkit.search.engine.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.search.engine.SearchEngine
import com.drinkit.search.engine.SearchIndexes
import com.drinkit.search.engine.infra.jooq.JooqRebuildState
import com.drinkit.search.engine.infra.meilisearch.MeilisearchEngine
import com.drinkit.search.engine.infra.meilisearch.MeilisearchIndexes
import com.drinkit.search.engine.infra.meilisearch.MeilisearchTasks
import com.drinkit.search.engine.infra.rebuild.Rebuilds
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

@AutoConfiguration(after = [JooqAutoConfiguration::class])
@ConditionalOnBean(DSLContext::class)
@ConditionalOnProperty(prefix = "starters.search-engine", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SearchEngineProperties::class)
internal class SearchEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun meilisearchClient(properties: SearchEngineProperties): Client =
        Client(Config(properties.meilisearch.url, properties.meilisearch.apiKey))

    // A bean, for its transactions
    @Bean
    @ConditionalOnMissingBean
    fun rebuildState(dsl: DSLContext): JooqRebuildState = JooqRebuildState(dsl)

    @Bean
    @ConditionalOnMissingBean
    @Suppress("LongParameterList") // What a rebuild needs, which the application provides
    fun searchIndexes(
        client: Client,
        jsonMapper: JsonMapper,
        state: JooqRebuildState,
        publisher: PlatformEventPublisher,
        transactionManager: PlatformTransactionManager,
        clock: Clock,
        properties: SearchEngineProperties,
    ): SearchIndexes {
        val rebuilds = Rebuilds(
            state,
            publisher,
            TransactionTemplate(transactionManager),
            clock,
            properties.rebuild.batchSize,
            properties.rebuild.restartAfter,
        )

        return MeilisearchIndexes(client, MeilisearchTasks(client, properties.taskTimeout), jsonMapper, rebuilds)
    }

    @Bean
    @ConditionalOnMissingBean
    fun searchEngine(client: Client, indexes: SearchIndexes, jsonMapper: JsonMapper): SearchEngine =
        MeilisearchEngine(client, indexes, jsonMapper)
}
