package com.drinkit.search.engine

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@TechStarterTool
interface SearchIndexes {

    fun <T : Any> declare(
        name: String,
        type: KType,
        source: SearchDocumentSource<T>,
        primaryKey: String,
        settings: String,
    ): SearchIndex<T>

    fun all(): List<SearchIndex<*>>
}

// Only a reified type gives the full type of a document, generics included, for its JSON to be read back
@TechStarterTool
inline fun <reified T : Any> SearchIndexes.declare(
    name: String,
    source: SearchDocumentSource<T>,
    primaryKey: String = "id",
    settings: String = "search/$name.json",
): SearchIndex<T> = declare(name, typeOf<T>(), source, primaryKey, settings)

@TechStarterTool
interface SearchDocumentSource<T : Any> {

    // Ids in a stable order, the page after the id given: a rebuild reads every id of the source this way
    fun ids(after: String?, limit: Int): List<String>

    // The documents of these ids that still exist
    fun documents(ids: List<String>): List<T>
}
