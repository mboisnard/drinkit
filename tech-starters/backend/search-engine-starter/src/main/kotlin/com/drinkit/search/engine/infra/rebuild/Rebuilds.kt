package com.drinkit.search.engine.infra.rebuild

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.search.engine.RebuildProgress
import com.drinkit.search.engine.SearchDocumentSource
import com.drinkit.search.engine.infra.SearchEngineProperties
import com.drinkit.search.engine.infra.meilisearch.MeilisearchIndex
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// A rebuild fills <index>_next batch by batch, on whichever instance gets each batch, then swaps it with the index
internal class Rebuilds(
    private val state: JooqRebuildState,
    private val publisher: PlatformEventPublisher,
    private val transactions: TransactionTemplate,
    private val clock: Clock,
    private val settings: SearchEngineProperties.Rebuild,
) {

    private val logger = KotlinLogging.logger {}

    // Meilisearch runs one task at a time: loading batches side by side only lengthens its queue past the timeout
    private val loading = ReentrantLock()

    // Claimed before the next index is touched, so that two instances that start a rebuild at once never drop the
    // next index of the other
    fun start(index: MeilisearchIndex<*>): RebuildProgress {
        val rebuild = UUID.randomUUID()
        val now = OffsetDateTime.now(clock)
        val claimed = transactions.execute { _ ->
            restartIfStale(index.name, now)
            state.claim(index.name, rebuild, now)
        } == true
        if (!claimed) return state.progress(index.name) ?: start(index)

        index.next.drop()
        index.next.createWithSettings()
        // The swap needs both indexes: the engine may have lost the live one, or never received it at startup
        index.live.createWithSettings()
        val batches = idBatches(index.source)
        transactions.executeWithoutResult { _ -> state.count(index.name, rebuild, batches.size) }
        batches.forEachIndexed { batch, ids ->
            publisher.publish(SearchIndexBatchRequested(index.name, rebuild, batch.toLong(), ids))
        }
        swapIfDone(index, rebuild)
        return state.progress(index.name) ?: RebuildProgress(batches.size.toLong(), batches.size.toLong())
    }

    fun process(index: MeilisearchIndex<*>, batch: SearchIndexBatchRequested) {
        if (!state.runs(index.name, batch.rebuild)) return
        loading.withLock {
            index.next.save(index.source.documents(batch.ids))
            // A document deleted while its batch was read would come back with the swap: its delete has passed
            index.next.delete(batch.ids - index.next.idsOf(index.source.documents(batch.ids)))
        }
        transactions.executeWithoutResult { _ ->
            if (state.markDone(index.name, batch.rebuild, batch.batch)) swapIfDone(index, batch.rebuild)
        }
    }

    fun progress(index: String): RebuildProgress? = state.progress(index)

    // A batch lost, an instance stopped or a swap left without its end: starting over is safe in each case, since the
    // next index holds either the old content once swapped, or what the new rebuild fills again
    private fun restartIfStale(index: String, now: OffsetDateTime) {
        if (state.restart(index, now.minus(settings.restartAfter))) {
            logger.warn { "The rebuild of index $index ran for longer than ${settings.restartAfter}, and starts over" }
        }
    }

    private fun idBatches(source: SearchDocumentSource<*>): List<List<String>> =
        generateSequence(source.ids(null, settings.batchSize)) { page -> source.ids(page.last(), settings.batchSize) }
            .takeWhile { it.isNotEmpty() }
            .toList()

    // The row goes in a transaction that ends once the swap is done and the old content dropped: until then, writes
    // still reach both indexes, and a rebuild that reaches no end starts over once it is stale
    private fun swapIfDone(index: MeilisearchIndex<*>, rebuild: UUID) {
        transactions.executeWithoutResult { _ ->
            if (state.claimSwap(index.name, rebuild)) {
                index.next.await(index.next.swapWith(index.live))
                index.next.drop()
            }
        }
    }
}

internal data class SearchIndexBatchRequested(
    val index: String,
    val rebuild: UUID,
    val batch: Long,
    val ids: List<String>,
) : PlatformEvent<SearchIndexBatchRequested>
