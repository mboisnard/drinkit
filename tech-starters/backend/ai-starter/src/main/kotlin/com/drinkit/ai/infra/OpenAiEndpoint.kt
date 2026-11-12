package com.drinkit.ai.infra

import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.OpenAiEmbeddingOptions.EncodingFormat
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer
import org.springframework.ai.openai.setup.OpenAiSetup

/** An empty key sends no Authorization header, which a LocalAI server without a key expects. */
internal class OpenAiEndpoint(endpoint: AiProperties.Endpoint, private val observationRegistry: ObservationRegistry) :
    ModelEndpoint {

    private val client = OpenAiSetup.setupSyncClient(
        endpoint.baseUrl, endpoint.apiKey, null, null, null, null, false, false, null,
        endpoint.timeout, endpoint.maxRetries, null, null, observationRegistry, null,
        listOf(OpenAiHttpClientBuilderCustomizer { it.interceptor(MissingEmbeddingUsage()) }),
    )

    private val clientAsync = OpenAiSetup.setupAsyncClient(
        endpoint.baseUrl, endpoint.apiKey, null, null, null, null, false, false, null,
        endpoint.timeout, endpoint.maxRetries, null, null, observationRegistry, null, emptyList(),
    )

    override fun chatModel(model: String): ChatModel = OpenAiChatModel.builder()
        .openAiClient(client)
        .openAiClientAsync(clientAsync)
        .options(OpenAiChatOptions.builder().model(model).build())
        .observationRegistry(observationRegistry)
        .build()

    // Floats, not the base64 the SDK asks for by default, so that a LocalAI answers as it documents
    override fun embeddingModel(model: String): EmbeddingModel = OpenAiEmbeddingModel.builder()
        .openAiClient(client)
        .options(OpenAiEmbeddingOptions.builder().model(model).encodingFormat(EncodingFormat.FLOAT).build())
        .observationRegistry(observationRegistry)
        .build()
}
