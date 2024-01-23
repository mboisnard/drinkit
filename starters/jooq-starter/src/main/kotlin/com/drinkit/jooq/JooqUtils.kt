package com.drinkit.jooq

import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.SelectConditionStep
import org.jooq.Table
import org.jooq.impl.DSL.row
import kotlin.streams.asSequence

const val DEFAULT_FETCH_SIZE = 1000

inline fun <reified R: Record> allFields(table: Table<R>) =
    row(*table.fields()).convertFrom { it.into(R::class.java) }

inline fun <reified R: Record, T: Record> SelectConditionStep<R>.fetchSequence(
    mapper: RecordMapper<R, T>,
    fetchSize: Int = DEFAULT_FETCH_SIZE,
): Sequence<T> = this.fetchSize(fetchSize)
    .fetchStream()
    .map { mapper.apply(it) }
    .asSequence()