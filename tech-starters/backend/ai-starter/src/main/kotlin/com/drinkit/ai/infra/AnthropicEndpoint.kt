package com.drinkit.ai.infra

import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.AnthropicSetup
import org.springframework.ai.chat.model.ChatModel

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
}
