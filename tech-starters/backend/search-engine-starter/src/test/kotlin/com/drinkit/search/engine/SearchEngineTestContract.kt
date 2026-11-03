package com.drinkit.search.engine

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

internal abstract class SearchEngineTestContract {

    abstract fun declareWines(name: String): SearchIndex<Wine>

    abstract fun engine(): SearchEngine

    @Test
    fun `lists a declared index with its number of documents`() {
        // Given
        val wines = declareWines("wines-${UUID.randomUUID()}")
        wines.save(listOf(Wine("1", "Margaux"), Wine("2", "Pomerol")))

        // When
        val information = engine().information()

        // Then
        information.indexes.single { it.name == wines.name } shouldBe
            IndexInformation(wines.name, documents = 2, indexing = false, declared = true, rebuild = null)
    }
}
