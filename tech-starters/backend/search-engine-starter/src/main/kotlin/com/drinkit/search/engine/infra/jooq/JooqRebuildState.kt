package com.drinkit.search.engine.infra.jooq

import com.drinkit.search.engine.RebuildProgress
import com.drinkit.search.engine.generated.jooq.tables.references.SEARCH_INDEX_REBUILD
import org.jooq.DSLContext
import org.jooq.impl.DSL.any
import org.jooq.impl.DSL.arrayAppend
import org.jooq.impl.DSL.cardinality
import org.jooq.impl.DSL.not
import org.jooq.impl.DSL.value
import org.jooq.impl.SQLDataType
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

// One row per running rebuild, which counts its batches done
@Transactional(propagation = Propagation.MANDATORY)
internal class JooqRebuildState(private val dsl: DSLContext) {

    // Waits for a rebuild of the same index that another transaction starts or swaps, then finds it there or gone
    @Suppress("ArrayPrimitive") // jOOQ maps a BIGINT[] to an Array<Long?>
    fun claim(index: String, rebuild: UUID, now: OffsetDateTime): Boolean = dsl.insertInto(SEARCH_INDEX_REBUILD)
        .set(SEARCH_INDEX_REBUILD.INDEX_NAME, index)
        .set(SEARCH_INDEX_REBUILD.REBUILD_ID, rebuild)
        .set(SEARCH_INDEX_REBUILD.TOTAL_BATCHES, 0)
        .set(SEARCH_INDEX_REBUILD.DONE_BATCHES, emptyArray())
        .set(SEARCH_INDEX_REBUILD.STARTED, now)
        .onConflictDoNothing()
        .execute() == 1

    fun count(index: String, rebuild: UUID, totalBatches: Int) {
        dsl.update(SEARCH_INDEX_REBUILD)
            .set(SEARCH_INDEX_REBUILD.TOTAL_BATCHES, totalBatches.toLong())
            .where(SEARCH_INDEX_REBUILD.INDEX_NAME.eq(index), SEARCH_INDEX_REBUILD.REBUILD_ID.eq(rebuild))
            .execute()
    }

    fun restart(index: String, startedBefore: OffsetDateTime): Boolean = dsl.deleteFrom(SEARCH_INDEX_REBUILD)
        .where(SEARCH_INDEX_REBUILD.INDEX_NAME.eq(index), SEARCH_INDEX_REBUILD.STARTED.lt(startedBefore))
        .execute() == 1

    // Read outside any transaction too: a write and an admin page ask whether a rebuild runs
    @Transactional(readOnly = true)
    fun progress(index: String): RebuildProgress? = dsl
        .select(cardinality(SEARCH_INDEX_REBUILD.DONE_BATCHES), SEARCH_INDEX_REBUILD.TOTAL_BATCHES)
        .from(SEARCH_INDEX_REBUILD)
        .where(SEARCH_INDEX_REBUILD.INDEX_NAME.eq(index))
        .fetchOne { (done, total) -> RebuildProgress((done ?: 0).toLong(), total ?: 0) }

    // A batch delivered again once its rebuild swapped, or one of a previous rebuild, has no next index to load into
    @Transactional(readOnly = true)
    fun runs(index: String, rebuild: UUID): Boolean = dsl.fetchExists(
        SEARCH_INDEX_REBUILD,
        SEARCH_INDEX_REBUILD.INDEX_NAME.eq(index),
        SEARCH_INDEX_REBUILD.REBUILD_ID.eq(rebuild),
    )

    // A batch delivered again, or one of a rebuild that restarted while it loaded, counts for nothing
    fun markDone(index: String, rebuild: UUID, batch: Long): Boolean = dsl.update(SEARCH_INDEX_REBUILD)
        .set(SEARCH_INDEX_REBUILD.DONE_BATCHES, arrayAppend(SEARCH_INDEX_REBUILD.DONE_BATCHES, batch))
        .where(
            SEARCH_INDEX_REBUILD.INDEX_NAME.eq(index),
            SEARCH_INDEX_REBUILD.REBUILD_ID.eq(rebuild),
            not(value(batch).eq(any(SEARCH_INDEX_REBUILD.DONE_BATCHES))),
        )
        .execute() == 1

    // Once every batch is done, the row goes to the one caller that swaps
    fun claimSwap(index: String, rebuild: UUID): Boolean = dsl.deleteFrom(SEARCH_INDEX_REBUILD)
        .where(
            SEARCH_INDEX_REBUILD.INDEX_NAME.eq(index),
            SEARCH_INDEX_REBUILD.REBUILD_ID.eq(rebuild),
            cardinality(SEARCH_INDEX_REBUILD.DONE_BATCHES)
                .cast(SQLDataType.BIGINT)
                .eq(SEARCH_INDEX_REBUILD.TOTAL_BATCHES),
        )
        .execute() == 1
}
