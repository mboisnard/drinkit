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
            require(model.endpoint in endpoints) {
                "Model $id names the endpoint ${model.endpoint}, which starters.ai.endpoints does not declare"
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

    data class Model(val endpoint: String, val name: String? = null)

    enum class Provider { OPENAI, ANTHROPIC }
}
