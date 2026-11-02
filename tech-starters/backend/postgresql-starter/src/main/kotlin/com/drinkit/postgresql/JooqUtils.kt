package com.drinkit.postgresql

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.Select
import org.jooq.Table
import org.jooq.impl.DSL.row
import org.postgresql.util.PSQLException
import org.springframework.dao.DataAccessException
import kotlin.streams.asSequence

const val DEFAULT_FETCH_SIZE = 1000

@TechStarterTool
inline fun <reified R : Record> allFields(table: Table<R>) = row(*table.fields()).convertFrom { it.into(R::class.java) }

// The rows come from an open cursor: read them inside the caller's transaction, whose end closes the cursor
@TechStarterTool
inline fun <reified R : Record, T> Select<R>.fetchSequence(
    fetchSize: Int = DEFAULT_FETCH_SIZE,
    mapper: RecordMapper<R, T>,
): Sequence<T> = this.fetchSize(fetchSize)
    .fetchStream()
    .map { mapper.apply(it) }
    .asSequence()

@TechStarterTool
fun DataAccessException.isEventSourcingSequenceException(primaryKeyName: String): Boolean {
    val mostSpecificCause = this.mostSpecificCause
    return mostSpecificCause is PSQLException &&
        mostSpecificCause.serverErrorMessage?.constraint == primaryKeyName
}
