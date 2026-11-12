package com.drinkit.ai.infra

import com.drinkit.ai.Embeddings
import com.drinkit.ai.EmbeddingsTestContract
import com.drinkit.ai.infra.AiProperties.Endpoint
import com.drinkit.ai.infra.AiProperties.Model
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class DefaultEmbeddingsTest : EmbeddingsTestContract() {

    private val server = FakeModelServer()

    override fun fetchEmbeddings(): Embeddings {
        val localAi = Endpoint(baseUrl = "${server.url}/v1", apiKeyRequired = false)
        val properties = AiProperties(
            endpoints = mapOf("localai" to localAi),
            models = mapOf(MODEL to Model(endpoint = "localai", dimensions = 3)),
        )
        return DefaultAiModels(properties, ObservationRegistry.NOOP).embeddings(MODEL)
    }

    override fun givenVector(text: String, vector: FloatArray) = server.embeds(MODEL, text, vector)

    override fun receivedTexts(): List<String> = server.embedded.flatten()

    @AfterEach
    fun stopServer() = server.close()

    @Test
    fun `masks the personal data of a text before it leaves`() {
        // Given
        server.embeds(MODEL, "Cellar of [email], card [card]", VECTOR)

        // When
        fetchEmbeddings().embed("Cellar of jeanne.martin@example.com, card 4111 1111 1111 1111")

        // Then
        receivedTexts() shouldBe listOf("Cellar of [email], card [card]")
    }

    @Test
    fun `embeds every text in one request`() {
        // Given
        server.embeds(MODEL, "Pommard 2019", VECTOR)
        server.embeds(MODEL, "Sauternes 2015", VECTOR)

        // When
        fetchEmbeddings().embed(listOf("Pommard 2019", "Sauternes 2015"))

        // Then
        server.embedded.size shouldBe 1
    }

    @Test
    fun `refuses an answer that holds fewer vectors than texts`() {
        // Given
        server.embeds(MODEL, "Pommard 2019", VECTOR)

        // When & Then
        shouldThrow<IllegalStateException> { fetchEmbeddings().embed(listOf("Pommard 2019", "Sauternes 2015")) }
            .message shouldContain "returned 1 vectors for 2 texts"
    }

    @Test
    fun `embeds through a server that reports no token usage, as LocalAI does`() {
        // Given
        server.reportsEmbeddingUsage = false
        server.embeds(MODEL, "Pommard 2019", VECTOR)

        // When
        val vector = fetchEmbeddings().embed("Pommard 2019")

        // Then
        vector.toList() shouldBe VECTOR.toList()
    }

    private companion object {
        const val MODEL = "qwen3-embedding-0.6b"
        val VECTOR = floatArrayOf(0.1f, 0.2f, 0.3f)
    }
}
