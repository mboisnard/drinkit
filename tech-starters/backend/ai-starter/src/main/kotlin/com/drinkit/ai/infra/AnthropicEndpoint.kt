package com.drinkit.ai.infra

import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.AnthropicSetup
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel

internal class AnthropicEndpoint(
    endpoint: AiProperties.Endpoint,
    private val observationRegistry: ObservationRegistry,
) : ModelEndpoint {

    private val client = AnthropicSetup.setupSyncClient(
        endpoint.baseUrl, endpoint.apiKey, endpoint.timeout, endpoint.maxRetries, null, null,
        observationRegistry, null, null, emptyList(),
    )

    private val clientAsync = AnthropicSetup.setupAsyncClient(
        endpoint.baseUrl, endpoint.apiKey, endpoint.timeout, endpoint.maxRetries, null, null,
        observationRegistry, null, null, emptyList(),
    )

    override fun chatModel(model: String): ChatModel = AnthropicChatModel.builder()
        .anthropicClient(client)
        .anthropicClientAsync(clientAsync)
        .options(AnthropicChatOptions.builder().model(model).build())
        .observationRegistry(observationRegistry)
        .build()

    // AiProperties refuses an embedding model on an Anthropic endpoint
    override fun embeddingModel(model: String): EmbeddingModel =
        throw UnsupportedOperationException("Anthropic serves no embedding model")
}
