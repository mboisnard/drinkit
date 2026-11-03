package com.drinkit.search.engine

import java.util.concurrent.ConcurrentSkipListMap

// The source of truth of a test index, read in pages of ids as a table would be
internal class Cellar(vararg wines: Wine) : SearchDocumentSource<Wine> {

    private val wines = ConcurrentSkipListMap(wines.associateBy { it.id })

    fun store(wine: Wine) {
        wines[wine.id] = wine
    }

    fun remove(id: String) {
        wines.remove(id)
    }

    override fun ids(after: String?, limit: Int): List<String> =
        (after?.let { wines.tailMap(it, false) } ?: wines).keys.take(limit)

    override fun documents(ids: List<String>): List<Wine> = ids.mapNotNull { wines[it] }
}
