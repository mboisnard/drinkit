package com.drinkit.ai.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_TIMEOUT_SECONDS = 60L
private const val DEFAULT_MAX_RETRIES = 2

@ConfigurationProperties("starters.ai")
internal data class AiProperties(
    val enabled: Boolean = true,
    val endpoints: Map<String, Endpoint> = emptyMap(),
    val models: Map<String, Model> = emptyMap(),
) {

    init {
        models.forEach { (id, model) ->
            val endpoint = requireNotNull(endpoints[model.endpoint]) {
                "Model $id names the endpoint ${model.endpoint}, which starters.ai.endpoints does not declare"
            }
            require(model.dimensions == null || endpoint.provider == Provider.OPENAI) {
                "Model $id is an embedding model, but endpoint ${model.endpoint} speaks the Anthropic protocol, " +
                    "which serves none"
            }
            require(model.dimensions == null || model.dimensions > 0) {
                "Model $id declares ${model.dimensions} dimensions, an embedding model needs at least one"
            }
        }
    }

    data class Endpoint(
        val provider: Provider = Provider.OPENAI,
        val baseUrl: String? = null,
        val apiKey: String = "",
        val apiKeyRequired: Boolean = true,
        val timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS),
        val maxRetries: Int = DEFAULT_MAX_RETRIES,
    ) {

        fun hasCredentials(): Boolean = apiKey.isNotBlank() || !apiKeyRequired
    }

    /** A model with [dimensions] is an embedding model. */
    data class Model(val endpoint: String, val name: String? = null, val dimensions: Int? = null)

    enum class Provider { OPENAI, ANTHROPIC }
}
