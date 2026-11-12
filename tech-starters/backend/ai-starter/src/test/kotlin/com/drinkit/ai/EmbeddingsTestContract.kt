package com.drinkit.ai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/** Run against embeddings of 3 dimensions. */
internal abstract class EmbeddingsTestContract {

    private val embeddings: Embeddings by lazy { fetchEmbeddings() }

    abstract fun fetchEmbeddings(): Embeddings

    abstract fun givenVector(text: String, vector: FloatArray)

    /** The texts the model received, in the order they came. */
    abstract fun receivedTexts(): List<String>

    @Test
    fun `embeds texts into vectors, in the order of the texts`() {
        // Given
        givenVector("Pommard 2019", floatArrayOf(0.1f, 0.2f, 0.3f))
        givenVector("Sauternes 2015", floatArrayOf(0.4f, 0.5f, 0.6f))

        // When
        val vectors = embeddings.embed(listOf("Sauternes 2015", "Pommard 2019"))

        // Then
        vectors.map { it.toList() } shouldBe listOf(listOf(0.4f, 0.5f, 0.6f), listOf(0.1f, 0.2f, 0.3f))
    }

    @Test
    fun `puts the instruction as is before each text`() {
        // Given
        givenVector("Instruct: Find the bottle\nQuery: Pommard 2019", floatArrayOf(0.1f, 0.2f, 0.3f))

        // When
        embeddings.embed("Pommard 2019", instruction = "Instruct: Find the bottle\nQuery: ")

        // Then
        receivedTexts() shouldBe listOf("Instruct: Find the bottle\nQuery: Pommard 2019")
    }

    @Test
    fun `embeds no text into no vector`() {
        // When
        val vectors = embeddings.embed(emptyList())

        // Then
        vectors.shouldBeEmpty()
    }

    @Test
    fun `refuses a vector whose size differs from the model's dimension, naming both`() {
        // Given
        givenVector("Pommard 2019", floatArrayOf(0.1f, 0.2f))

        // When & Then
        shouldThrow<IllegalStateException> { embeddings.embed("Pommard 2019") }
            .message shouldContain "a vector of 2 dimensions, its catalog declares 3"
    }
}
