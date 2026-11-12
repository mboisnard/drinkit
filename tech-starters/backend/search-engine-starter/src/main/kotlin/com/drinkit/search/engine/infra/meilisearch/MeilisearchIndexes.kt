package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.search.engine.SearchDocumentSource
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.SearchIndexes
import com.drinkit.search.engine.infra.rebuild.Rebuilds
import com.drinkit.search.engine.infra.rebuild.SearchIndexBatchRequested
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.json.GsonJsonHandler
import com.meilisearch.sdk.model.FilterableAttributesConfig
import com.meilisearch.sdk.model.FilterableAttributesFeatures
import com.meilisearch.sdk.model.FilterableAttributesFilter
import com.meilisearch.sdk.model.Settings
import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KType
import kotlin.reflect.javaType

internal class MeilisearchIndexes(
    private val client: Client,
    private val tasks: MeilisearchTasks,
    private val jsonMapper: JsonMapper,
    private val rebuilds: Rebuilds,
) : SearchIndexes {

    private val declared = CopyOnWriteArrayList<MeilisearchIndex<*>>()

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> declare(
        name: String,
        type: KType,
        source: SearchDocumentSource<T>,
        primaryKey: String,
        settings: String,
    ): SearchIndex<T> {
        require(declared.none { it.name == name }) { "Index $name is declared twice" }
        val definition = IndexDefinition(
            name,
            primaryKey,
            settingsOf(settings, primaryKey),
            jsonMapper.constructType(type.javaType),
            source,
        )
        return MeilisearchIndex(definition, client, tasks, jsonMapper, rebuilds).also { declared += it }
    }

    override fun all(): List<SearchIndex<*>> = declared.toList()

    @PlatformEventHandler(name = "search-engine.rebuild-batch")
    fun onBatchRequested(event: SearchIndexBatchRequested) {
        val index = declared.firstOrNull { it.name == event.index }
        rebuilds.process(requireNotNull(index) { "Index ${event.index} is not declared on this instance" }, event)
    }

    // The SDK reads its own Settings with Gson, which drops a key that this version of the SDK does not know.
    // Deleting documents by id filters on the primary key, so each index can filter on it
    private fun settingsOf(path: String, primaryKey: String): Settings {
        val file = ClassPathResource(path)
        require(file.exists()) { "No settings file $path on the classpath" }

        val settings = GsonJsonHandler().decode(file.getContentAsString(Charsets.UTF_8), Settings::class.java)
        val filterable = settings.filterableAttributesConfig ?: emptyArray()
        val byEquality = FilterableAttributesFeatures(false, FilterableAttributesFilter(true, false))
        return settings.setFilterableAttributesConfig(
            filterable + FilterableAttributesConfig(arrayOf(primaryKey), byEquality),
        )
    }
}
