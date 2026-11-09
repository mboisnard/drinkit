package com.drinkit.search.engine

import tools.jackson.module.kotlin.jacksonMapperBuilder
import kotlin.reflect.KType
import kotlin.reflect.javaType

class InMemorySearchIndexes :
    SearchIndexes,
    SearchEngine {

    private val jsonMapper = jacksonMapperBuilder().build()

    private val declared = mutableListOf<InMemorySearchIndex<*>>()

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> declare(
        name: String,
        type: KType,
        source: SearchDocumentSource<T>,
        primaryKey: String,
        settings: String,
    ): SearchIndex<T> {
        require(declared.none { it.name == name }) { "Index $name is declared twice" }

        return InMemorySearchIndex(name, primaryKey, source, jsonMapper.constructType(type.javaType), jsonMapper)
            .also { declared += it }
    }

    override fun all(): List<SearchIndex<*>> = declared.toList()

    override fun information() = SearchEngineInformation(
        version = "none",
        size = 0,
        indexes = declared.map {
            IndexInformation(
                it.name,
                it.size(),
                indexing = false,
                declared = true,
                rebuild = null,
            )
        },
    )
}
