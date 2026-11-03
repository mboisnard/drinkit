package com.drinkit.search.engine

internal class InMemorySearchIndexTest : SearchIndexTestContract() {

    override fun declareWines(name: String, source: SearchDocumentSource<Wine>, primaryKey: String): SearchIndex<Wine> =
        InMemorySearchIndexes().declare(name, source, primaryKey)
}
