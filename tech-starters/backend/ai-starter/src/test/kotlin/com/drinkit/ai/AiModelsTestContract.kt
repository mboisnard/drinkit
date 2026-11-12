package com.drinkit.ai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal abstract class AiModelsTestContract {

    private val aiModels: AiModels by lazy { fetchAiModels() }

    abstract fun fetchAiModels(): AiModels

    abstract fun givenModel(model: String, answer: String)

    /** An embedding model of 3 dimensions. */
    abstract fun givenEmbeddingModel(model: String)

    @Test
    fun `a model of the catalog is available`() {
        // Given
        givenModel("qwen3-vl-4b", answer = "Bordeaux")

        // When
        val available = aiModels.isAvailable("qwen3-vl-4b")

        // Then
        available shouldBe true
    }

    @Test
    fun `an unknown model is unavailable`() {
        // When
        val available = aiModels.isAvailable("unknown-model")

        // Then
        available shouldBe false
    }

    @Test
    fun `answers through the chat client of the model`() {
        // Given
        givenModel("qwen3-vl-4b", answer = "Bordeaux")

        // When
        val answer = aiModels.chatClient("qwen3-vl-4b").prompt().user("Which region?").call().content()

        // Then
        answer shouldBe "Bordeaux"
    }

    @Test
    fun `refuses the chat client of an unavailable model, naming it`() {
        // When & Then
        shouldThrow<ModelUnavailableException> { aiModels.chatClient("unknown-model") }.model shouldBe "unknown-model"
    }

    @Test
    fun `an embedding model of the catalog is available, with its dimension`() {
        // Given
        givenEmbeddingModel("qwen3-embedding-0.6b")

        // When
        val embeddings = aiModels.embeddings("qwen3-embedding-0.6b")

        // Then
        embeddings.dimensions shouldBe 3
        aiModels.isAvailable("qwen3-embedding-0.6b") shouldBe true
    }

    @Test
    fun `refuses the embeddings of a model that is no embedding model, naming it`() {
        // Given
        givenModel("qwen3-vl-4b", answer = "Bordeaux")

        // When & Then
        listOf("qwen3-vl-4b", "unknown-model").forAll { model ->
            shouldThrow<ModelUnavailableException> { aiModels.embeddings(model) }.model shouldBe model
        }
    }
}
