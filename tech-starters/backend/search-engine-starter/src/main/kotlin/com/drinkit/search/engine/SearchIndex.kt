package com.drinkit.search.engine

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface SearchIndex<T : Any> {

    val name: String

    fun save(document: T)

    fun save(documents: Collection<T>)

    fun delete(id: String)

    fun delete(ids: Collection<String>)

    fun find(id: String): T?

    // Starts a rebuild from the source of the index without waiting for its batches, or gives the running one
    fun rebuild(): RebuildProgress

    fun rebuildProgress(): RebuildProgress?
}

data class RebuildProgress(val doneBatches: Long, val totalBatches: Long)

@TechStarterTool
class SearchIndexingFailed(val index: String, val code: String?, message: String) :
    RuntimeException("Index $index: $message")
