package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.search.engine.RebuildProgress
import com.drinkit.search.engine.SearchDocumentSource
import com.drinkit.search.engine.SearchIndex
import com.drinkit.search.engine.infra.rebuild.Rebuilds
import com.drinkit.search.engine.infra.rebuild.RebuiltIndex
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.exceptions.MeilisearchApiException
import com.meilisearch.sdk.model.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import tools.jackson.databind.JavaType
import tools.jackson.databind.json.JsonMapper

private val NOT_FOUND = setOf("document_not_found", "index_not_found")

internal class MeilisearchIndex<T : Any>(
    private val definition: IndexDefinition<T>,
    private val client: Client,
    tasks: MeilisearchTasks,
    private val jsonMapper: JsonMapper,
    private val rebuilds: Rebuilds,
) : SearchIndex<T>,
    RebuiltIndex {

    private val logger = KotlinLogging.logger {}

    override val name = definition.name

    override val source = definition.source

    val live = EngineIndex(name, definition, client, tasks, jsonMapper)

    val next = EngineIndex("${name}_next", definition, client, tasks, jsonMapper)

    // Creates the index when missing, and applies a changed settings file. The application starts even when the engine
    // does not answer: a first write then creates the index without its settings, until the next start or rebuild
    init {
        runCatching { sendSettings() }
            .onFailure { logger.error(it) { "The settings of index $name could not be sent" } }
    }

    override fun save(document: T) = save(listOf(document))

    // During a rebuild, a write goes to the index being filled too, so that the swap does not lose it. The next index
    // first: a swap between the two writes then finds the write in the content it makes live
    override fun save(documents: Collection<T>) {
        if (rebuilds.progress(name) != null) next.save(documents)
        live.save(documents)
    }

    override fun delete(id: String) = delete(listOf(id))

    override fun delete(ids: Collection<String>) {
        if (rebuilds.progress(name) != null) next.delete(ids)
        live.delete(ids)
    }

    override fun find(id: String): T? = try {
        jsonMapper.readValue(client.index(name).getRawDocument(id), definition.type)
    } catch (exception: MeilisearchApiException) {
        if (exception.code in NOT_FOUND) null else throw exception
    }

    override fun rebuild(): RebuildProgress = rebuilds.start(this)

    override fun rebuildProgress(): RebuildProgress? = rebuilds.progress(name)

    override fun prepareNext() {
        next.drop()
        next.createWithSettings()
        // The swap needs both indexes: the engine may have lost the live one, or never received it at startup
        live.createWithSettings()
    }

    override fun loadIntoNext(ids: List<String>) {
        next.save(source.documents(ids))
        // A document deleted while its batch was read would come back with the swap: its delete has passed
        next.delete(ids - next.idsOf(source.documents(ids)))
    }

    override fun swapNext() {
        next.await(next.swapWith(live))
        next.drop()
    }

    // Sent without waiting for the engine, so that a busy engine does not hold the startup back. A running rebuild gets
    // them too, or its swap would bring the settings of the previous start back
    private fun sendSettings() {
        val indexes = if (rebuilds.progress(name) != null) listOf(live, next) else listOf(live)
        val sent = indexes.map { it to it.sendSettings() }
        Thread.ofVirtual().start {
            sent.forEach { (index, task) ->
                runCatching { index.await(task) }
                    .onFailure { logger.error(it) { "The settings of index ${index.uid} could not be applied" } }
            }
        }
    }
}

internal data class IndexDefinition<T : Any>(
    val name: String,
    val primaryKey: String,
    val settings: Settings,
    val type: JavaType,
    val source: SearchDocumentSource<T>,
)
