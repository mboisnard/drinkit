package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.search.engine.IndexInformation
import com.drinkit.search.engine.SearchEngine
import com.drinkit.search.engine.SearchEngineInformation
import com.drinkit.search.engine.SearchIndexes
import com.meilisearch.sdk.Client
import tools.jackson.databind.json.JsonMapper

internal class MeilisearchEngine(
    private val client: Client,
    private val indexes: SearchIndexes,
    private val jsonMapper: JsonMapper,
) : SearchEngine {

    override fun information(): SearchEngineInformation {
        val stats = client.stats
        val declared = indexes.all().associateBy { it.name }

        return SearchEngineInformation(
            version = jsonMapper.readTree(client.version).required("pkgVersion").asString(),
            size = stats.databaseSize,
            indexes = stats.indexes.map { (name, index) ->
                IndexInformation(
                    name = name,
                    documents = index.numberOfDocuments,
                    indexing = index.isIndexing,
                    declared = name in declared,
                    rebuild = declared[name]?.rebuildProgress(),
                )
            }.sortedBy { it.name },
        )
    }
}
