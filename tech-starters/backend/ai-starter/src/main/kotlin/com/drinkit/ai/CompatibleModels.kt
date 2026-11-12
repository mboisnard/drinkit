package com.drinkit.ai

import com.drinkit.ai.guardrails.AiRefusedException
import com.drinkit.ai.guardrails.GuardrailFindings
import com.drinkit.documentation.tech.starter.TechStarterTool
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import tools.jackson.core.JacksonException
import kotlin.reflect.KClass

/** Asks the models of a list in order, and answers with the first one that answers. */
@TechStarterTool
class CompatibleModels(private val aiModels: AiModels) {

    private val logger = KotlinLogging.logger {}

    @Suppress("TooGenericExceptionCaught") // The SDKs throw their own unchecked exceptions, which no common type covers
    fun <T : Any> call(models: List<String>, request: (ChatClient) -> T?): ModelAnswer<T> {
        val causes = linkedMapOf<String, Throwable>()

        for (model in models) {
            if (!aiModels.isAvailable(model)) {
                causes[model] = ModelUnavailableException(model)
                continue
            }

            try {
                val findings = GuardrailFindings()
                val client = aiModels.chatClient(model).mutate()
                    .defaultAdvisors { it.param(GuardrailFindings.KEY, findings) }
                    .build()

                val value = request(client) ?: error("Model $model answered nothing")
                return ModelAnswer(model, value, findings.all)
            } catch (failure: RuntimeException) {
                stopOnRefusal(failure)
                logger.warn {
                    "Model $model failed, trying the next one: ${failure.javaClass.name}: ${failure.message}"
                }
                causes[model] = failure
            }
        }

        throw AllModelsFailedException(causes)
    }

    // A refusal of the guardrails stands for every model: asking the next one would only repeat it
    private fun stopOnRefusal(failure: RuntimeException) {
        if (failure is AiRefusedException) throw failure
    }

    /**
     * Asks again, with the validation error, a model whose answer does not match [type]. A model that still answers
     * wrong fails with an [InvalidAnswerException], and the next model of the list is asked.
     */
    fun <T : Any> entity(
        models: List<String>,
        type: KClass<T>,
        prompt: (ChatClient) -> ChatClient.ChatClientRequestSpec,
    ): ModelAnswer<T> = call(models) { client ->
        try {
            prompt(client).call().entity(type.java) { it.validateSchema() }
        } catch (invalid: JacksonException) {
            throw InvalidAnswerException(fieldOf(invalid), invalid)
        }
    }

    private fun fieldOf(invalid: JacksonException): String = invalid.path
        .joinToString(".") { reference -> reference.propertyName ?: "[${reference.index}]" }
        .replace(".[", "[")
        .ifEmpty { "$" }
}
