package com.drinkit.search.engine.infra.rebuild

import com.drinkit.search.engine.SearchDocumentSource

// What a rebuild needs of the index of an engine: a next index to fill, then to swap with the live one
internal interface RebuiltIndex {

    val name: String

    val source: SearchDocumentSource<*>

    // Drops a next index left behind, then creates the next and the live index with their settings
    fun prepareNext()

    // Copies the documents of these ids from the source, and leaves out those the source no longer has
    fun loadIntoNext(ids: List<String>)

    fun swapNext()
}
