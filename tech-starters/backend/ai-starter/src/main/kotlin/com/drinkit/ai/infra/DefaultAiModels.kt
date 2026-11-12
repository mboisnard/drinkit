package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import com.drinkit.ai.ModelUnavailableException
import com.drinkit.ai.infra.AiProperties.Provider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer

/** Builds at startup the chat client of each model of the catalog whose endpoint has its key. */
internal class DefaultAiModels(
    properties: AiProperties,
    private val observationRegistry: ObservationRegistry,
    private val customizers: List<ChatClientBuilderCustomizer> = emptyList(),
) : AiModels {

    private val logger = KotlinLogging.logger {}

    private val clients: Map<String, ChatClient> = if (properties.enabled) {
        val endpoints = mutableMapOf<String, ModelEndpoint>()

        properties.models.mapNotNull { (id, model) ->
            val endpoint = properties.endpoints.getValue(model.endpoint)

            if (endpoint.hasCredentials()) {
                val modelEndpoint = endpoints.getOrPut(model.endpoint) { connect(endpoint) }
                id to chatClient(modelEndpoint, model.name ?: id)
            } else {
                logger.info { "Model $id is unavailable: endpoint ${model.endpoint} has no API key" }
                null
            }
        }.toMap()
    } else {
        emptyMap()
    }

    override fun isAvailable(model: String): Boolean = model in clients

    override fun chatClient(model: String): ChatClient = clients[model] ?: throw ModelUnavailableException(model)

    private fun connect(endpoint: AiProperties.Endpoint): ModelEndpoint = when (endpoint.provider) {
        Provider.OPENAI -> OpenAiEndpoint(endpoint, observationRegistry)
        Provider.ANTHROPIC -> AnthropicEndpoint(endpoint, observationRegistry)
    }

    private fun chatClient(endpoint: ModelEndpoint, name: String): ChatClient =
        ChatClient.builder(endpoint.chatModel(name), observationRegistry, null, null)
            .apply { customizers.forEach { it.customize(this) } }
            .build()
}
