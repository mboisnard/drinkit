package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.search.engine.SearchIndexingFailed
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.model.SwapIndexesParams
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

// One index of the engine: the live index of a SearchIndex, or the next one a rebuild fills
internal class EngineIndex(
    val uid: String,
    private val definition: IndexDefinition<*>,
    private val client: Client,
    private val tasks: MeilisearchTasks,
    private val jsonMapper: JsonMapper,
) {

    fun createWithSettings() = await(sendSettings())

    // Creates the index when missing. Meilisearch runs the tasks in the order they came, so a write sent after this
    // one finds the settings applied
    fun sendSettings(): Int = client.index(uid).updateSettings(definition.settings).taskUid

    fun save(documents: Collection<*>) {
        await(client.index(uid).addDocuments(jsonMapper.writeValueAsString(documents), definition.primaryKey).taskUid)
    }

    // An index gone holds no document: the next index of a rebuild is dropped a moment before its rebuild ends
    fun delete(ids: Collection<String>) {
        if (ids.isNotEmpty()) {
            ignoring("index_not_found") { await(client.index(uid).deleteDocuments(ids.toList()).taskUid) }
        }
    }

    fun idsOf(documents: Collection<*>): Set<String> =
        documents.map { jsonMapper.valueToTree<JsonNode>(it).required(definition.primaryKey).asString() }.toSet()

    fun drop() {
        ignoring("index_not_found") { await(client.deleteIndex(uid).taskUid) }
    }

    fun swapWith(other: EngineIndex): Int =
        client.swapIndexes(arrayOf(SwapIndexesParams().setIndexes(arrayOf(uid, other.uid)))).taskUid

    fun await(task: Int) = tasks.await(uid, task)

    // A task that failed with this code changed nothing the caller needs
    fun ignoring(code: String, write: () -> Unit) {
        try {
            write()
        } catch (failure: SearchIndexingFailed) {
            if (failure.code != code) throw failure
        }
    }
}
