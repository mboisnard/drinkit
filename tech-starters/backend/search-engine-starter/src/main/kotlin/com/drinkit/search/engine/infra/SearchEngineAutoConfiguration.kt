package com.drinkit.search.engine.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.search.engine.SearchEngine
import com.drinkit.search.engine.SearchIndexes
import com.drinkit.search.engine.infra.meilisearch.MeilisearchEngine
import com.drinkit.search.engine.infra.meilisearch.MeilisearchIndexes
import com.drinkit.search.engine.infra.meilisearch.MeilisearchTasks
import com.drinkit.search.engine.infra.rebuild.JooqRebuildState
import com.drinkit.search.engine.infra.rebuild.Rebuilds
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfiguration
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
@ConditionalOnProperty(prefix = "starters.search-engine", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SearchEngineProperties::class)
internal class SearchEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun meilisearchClient(properties: SearchEngineProperties): Client =
        Client(Config(properties.meilisearch.url, properties.meilisearch.apiKey))

    @Bean
    @ConditionalOnMissingBean
    fun meilisearchTasks(client: Client, properties: SearchEngineProperties): MeilisearchTasks =
        MeilisearchTasks(client, properties.taskTimeout)

    @Bean
    @ConditionalOnMissingBean
    fun rebuildState(dsl: DSLContext): JooqRebuildState = JooqRebuildState(dsl)

    @Bean
    @ConditionalOnMissingBean
    fun rebuilds(
        state: JooqRebuildState,
        publisher: PlatformEventPublisher,
        transactionManager: PlatformTransactionManager,
        clock: Clock,
        properties: SearchEngineProperties,
    ): Rebuilds = Rebuilds(state, publisher, TransactionTemplate(transactionManager), clock, properties.rebuild)

    @Bean
    @ConditionalOnMissingBean
    fun searchIndexes(
        client: Client,
        tasks: MeilisearchTasks,
        jsonMapper: JsonMapper,
        rebuilds: Rebuilds,
    ): SearchIndexes = MeilisearchIndexes(client, tasks, jsonMapper, rebuilds)

    @Bean
    @ConditionalOnMissingBean
    fun searchEngine(client: Client, indexes: SearchIndexes, jsonMapper: JsonMapper): SearchEngine =
        MeilisearchEngine(client, indexes, jsonMapper)
}
