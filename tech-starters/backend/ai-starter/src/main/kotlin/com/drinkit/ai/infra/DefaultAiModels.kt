package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import com.drinkit.ai.Embeddings
import com.drinkit.ai.ModelUnavailableException
import com.drinkit.ai.infra.AiProperties.Model
import com.drinkit.ai.infra.AiProperties.Provider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer

/**
 * Builds at startup each model of the catalog whose endpoint has its key: the embeddings of a model that declares its
 * dimensions, the chat client of the others.
 */
internal class DefaultAiModels(
    private val properties: AiProperties,
    private val observationRegistry: ObservationRegistry,
    private val customizers: List<ChatClientBuilderCustomizer> = emptyList(),
) : AiModels {

    private val logger = KotlinLogging.logger {}

    private val endpoints = mutableMapOf<String, ModelEndpoint>()

    private val available: Map<String, Model> =
        if (properties.enabled) properties.models.filter { (id, model) -> hasKey(id, model) } else emptyMap()

    private val clients: Map<String, ChatClient> = available
        .filterValues { it.dimensions == null }
        .mapValues { (id, model) -> chatClient(endpointOf(model), model.name ?: id) }

    private val embeddings: Map<String, Embeddings> = available.mapNotNull { (id, model) ->
        model.dimensions?.let { id to DefaultEmbeddings(id, endpointOf(model).embeddingModel(model.name ?: id), it) }
    }.toMap()

    override fun isAvailable(model: String): Boolean = model in clients || model in embeddings

    override fun chatClient(model: String): ChatClient = clients[model] ?: throw ModelUnavailableException(model)

    override fun embeddings(model: String): Embeddings = embeddings[model] ?: throw ModelUnavailableException(model)

    private fun hasKey(id: String, model: Model): Boolean {
        val hasKey = properties.endpoints.getValue(model.endpoint).hasCredentials()
        if (!hasKey) logger.info { "Model $id is unavailable: endpoint ${model.endpoint} has no API key" }

        return hasKey
    }

    private fun endpointOf(model: Model): ModelEndpoint = endpoints.getOrPut(model.endpoint) {
        val endpoint = properties.endpoints.getValue(model.endpoint)
        when (endpoint.provider) {
            Provider.OPENAI -> OpenAiEndpoint(endpoint, observationRegistry)
            Provider.ANTHROPIC -> AnthropicEndpoint(endpoint, observationRegistry)
        }
    }

    private fun chatClient(endpoint: ModelEndpoint, name: String): ChatClient =
        ChatClient.builder(endpoint.chatModel(name), observationRegistry, null, null)
            .apply { customizers.forEach { it.customize(this) } }
            .build()
}
