package com.drinkit.search.engine

import tools.jackson.module.kotlin.jacksonMapperBuilder
import kotlin.reflect.KType
import kotlin.reflect.javaType

class InMemorySearchIndexes : SearchIndexes {

    private val jsonMapper = jacksonMapperBuilder().build()

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> declare(
        name: String,
        type: KType,
        source: SearchDocumentSource<T>,
        primaryKey: String,
        settings: String,
    ): SearchIndex<T> = InMemorySearchIndex(
        name,
        primaryKey,
        source,
        jsonMapper.constructType(type.javaType),
        jsonMapper,
    )
}
