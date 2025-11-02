package com.drinkit.postgresql.jooq

import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.Select
import org.jooq.Table
import org.jooq.impl.DSL.row
import org.postgresql.util.PSQLException
import org.springframework.dao.DataAccessException
import kotlin.streams.asSequence

const val DEFAULT_FETCH_SIZE = 1000

inline fun <reified R : Record> allFields(table: Table<R>) =
    row(*table.fields()).convertFrom { it.into(R::class.java) }

inline fun <reified R : Record, T> Select<R>.fetchSequence(
    fetchSize: Int = DEFAULT_FETCH_SIZE,
    mapper: RecordMapper<R, T>,
): Sequence<T> = this.fetchSize(fetchSize)
    .fetchStream()
    .map { mapper.apply(it) }
    .asSequence()

fun DataAccessException.isEventSourcingSequenceException(primaryKeyName: String): Boolean {
    val mostSpecificCause = this.mostSpecificCause
    return mostSpecificCause is PSQLException &&
            mostSpecificCause.serverErrorMessage?.constraint == primaryKeyName
}