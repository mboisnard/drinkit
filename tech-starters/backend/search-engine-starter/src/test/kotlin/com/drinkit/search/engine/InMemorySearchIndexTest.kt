package com.drinkit.search.engine

internal class InMemorySearchIndexTest : SearchIndexTestContract() {

    private val indexes = InMemorySearchIndexes()

    override fun declareWines(name: String, source: SearchDocumentSource<Wine>, primaryKey: String): SearchIndex<Wine> =
        indexes.declare(name, source, primaryKey)
}
