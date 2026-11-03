package com.drinkit.search.engine

import tools.jackson.databind.JavaType
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.ConcurrentHashMap

private const val REBUILD_BATCH_SIZE = 1_000

// The ids Meilisearch accepts
private val ACCEPTED_ID = Regex("[A-Za-z0-9_-]{1,511}")

// Keeps the JSON of each document, as an engine does, so that a document reads back as it would from one
class InMemorySearchIndex<T : Any>(
    override val name: String,
    private val primaryKey: String,
    private val source: SearchDocumentSource<T>,
    private val type: JavaType,
    private val jsonMapper: JsonMapper,
) : SearchIndex<T> {

    private val documents = ConcurrentHashMap<String, JsonNode>()

    override fun save(document: T) = save(listOf(document))

    // Refuses the whole list for one document the engine would refuse, as the engine does
    override fun save(documents: Collection<T>) {
        val byId = documents.map { jsonMapper.valueToTree<JsonNode>(it) }.associateBy(::idOf)
        this.documents.putAll(byId)
    }

    override fun delete(id: String) = delete(listOf(id))

    override fun delete(ids: Collection<String>) {
        documents.keys.removeAll(ids.toSet())
    }

    fun size(): Long = documents.size.toLong()

    private fun idOf(json: JsonNode): String {
        val id = json.get(primaryKey)?.asString()
            ?: throw SearchIndexingFailed(name, "missing_document_id", "a document has no $primaryKey")
        if (!ACCEPTED_ID.matches(id)) throw SearchIndexingFailed(name, "invalid_document_id", "$id is not a valid id")
        return id
    }

    override fun find(id: String): T? = documents[id]?.let { jsonMapper.treeToValue(it, type) }

    // Done at once: the double has no batch to hand to another instance
    override fun rebuild(): RebuildProgress {
        val batches = generateSequence(
            source.ids(null, REBUILD_BATCH_SIZE),
        ) { source.ids(it.last(), REBUILD_BATCH_SIZE) }
            .takeWhile { it.isNotEmpty() }
            .toList()
        documents.clear()
        batches.forEach { save(source.documents(it)) }
        return RebuildProgress(batches.size.toLong(), batches.size.toLong())
    }

    override fun rebuildProgress(): RebuildProgress? = null
}
