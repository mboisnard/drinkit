package com.drinkit.search.engine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal abstract class SearchIndexTestContract {

    protected val cellar = Cellar()

    protected val wines: SearchIndex<Wine> by lazy { declareWines("wines-${UUID.randomUUID()}", cellar) }

    abstract fun declareWines(
        name: String,
        source: SearchDocumentSource<Wine>,
        primaryKey: String = "id",
    ): SearchIndex<Wine>

    @Test
    fun `finds a saved document by its id`() {
        // Given
        wines.save(Wine("1", "Margaux"))

        // When
        val found = wines.find("1")

        // Then
        found shouldBe Wine("1", "Margaux")
    }

    @Test
    fun `finds nothing for an id never saved`() {
        // When & Then
        wines.find("unknown").shouldBeNull()
    }

    @Test
    fun `replaces a document saved again with the same id`() {
        // Given
        wines.save(Wine("1", "Margaux"))

        // When
        wines.save(Wine("1", "Château Margaux"))

        // Then
        wines.find("1") shouldBe Wine("1", "Château Margaux")
    }

    @Test
    fun `saves every document of a list`() {
        // When
        wines.save(listOf(Wine("1", "Margaux"), Wine("2", "Pomerol")))

        // Then
        listOf(wines.find("1"), wines.find("2")) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"))
    }

    @Test
    fun `finds no longer a deleted document`() {
        // Given
        wines.save(Wine("1", "Margaux"))

        // When
        wines.delete("1")

        // Then
        wines.find("1").shouldBeNull()
    }

    @Test
    fun `finds no longer the documents of a list of ids deleted`() {
        // Given
        wines.save(listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"), Wine("3", "Pauillac")))

        // When
        wines.delete(listOf("1", "2"))

        // Then
        listOf("1", "2", "3").map(wines::find) shouldBe listOf(null, null, Wine("3", "Pauillac"))
    }

    @Test
    fun `deletes only the id given, even one written like a filter`() {
        // Given
        wines.save(listOf(Wine("1", "Margaux"), Wine("2", "Pomerol")))

        // When
        wines.delete("1\"] OR id EXISTS OR id IN [\"2")

        // Then
        listOf("1", "2").map(wines::find) shouldBe listOf(Wine("1", "Margaux"), Wine("2", "Pomerol"))
    }

    @Test
    fun `deletes an id never saved without failing`() {
        // When
        wines.delete("unknown")

        // Then
        wines.find("unknown").shouldBeNull()
    }

    @Test
    fun `reads back a price, a date and a list as they were saved`() {
        // Given
        val wine = Wine("1", "Margaux", "89.5".toBigDecimal(), LocalDate.of(2019, 6, 1), listOf("merlot", "cabernet"))
        wines.save(wine)

        // When
        val found = wines.find("1")

        // Then
        found shouldBe wine
    }

    @Test
    fun `holds exactly the documents of its source once rebuilt`() {
        // Given
        wines.save(listOf(Wine("1", "Margaux"), Wine("3", "Gone")))
        cellar.store(Wine("1", "Château Margaux"))
        cellar.store(Wine("2", "Pomerol"))

        // When
        wines.rebuild()
        wines.awaitRebuilt()

        // Then
        listOf("1", "2", "3").map(wines::find) shouldBe listOf(Wine("1", "Château Margaux"), Wine("2", "Pomerol"), null)
    }

    @Test
    fun `holds no document once rebuilt from an empty source`() {
        // Given
        wines.save(Wine("1", "Margaux"))

        // When
        wines.rebuild()
        wines.awaitRebuilt()

        // Then
        wines.find("1").shouldBeNull()
    }

    @Test
    fun `refuses a document without its id`() {
        // Given
        val bySku = declareWines("wines-${UUID.randomUUID()}", cellar, primaryKey = "sku")

        // When
        val failure = shouldThrow<SearchIndexingFailed> { bySku.save(Wine("1", "Margaux")) }

        // Then
        failure.code shouldBe "missing_document_id"
    }

    @Test
    fun `refuses an index declared twice`() {
        // Given
        val name = "wines-${UUID.randomUUID()}"
        declareWines(name, cellar)

        // When
        val failure = shouldThrow<IllegalArgumentException> { declareWines(name, cellar) }

        // Then
        failure.message shouldBe "Index $name is declared twice"
    }

    @Test
    fun `refuses an id the engine does not accept`() {
        // When
        val failure = shouldThrow<SearchIndexingFailed> { wines.save(Wine("cellar:42", "Margaux")) }

        // Then
        failure.code shouldBe "invalid_document_id"
    }
}
