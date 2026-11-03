package com.drinkit.search.engine

internal class InMemorySearchEngineTest : SearchEngineTestContract() {

    private val indexes = InMemorySearchIndexes()

    override fun declareWines(name: String): SearchIndex<Wine> = indexes.declare(name, Cellar())

    override fun engine(): SearchEngine = indexes
}
