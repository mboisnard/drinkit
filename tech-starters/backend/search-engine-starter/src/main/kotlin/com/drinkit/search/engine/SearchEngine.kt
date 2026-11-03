package com.drinkit.search.engine

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface SearchEngine {

    fun information(): SearchEngineInformation
}

data class SearchEngineInformation(val version: String, val size: Long, val indexes: List<IndexInformation>)

// An index the application did not declare is the next index of a rebuild, or one left behind
data class IndexInformation(
    val name: String,
    val documents: Long,
    val indexing: Boolean,
    val declared: Boolean,
    val rebuild: RebuildProgress?,
)
