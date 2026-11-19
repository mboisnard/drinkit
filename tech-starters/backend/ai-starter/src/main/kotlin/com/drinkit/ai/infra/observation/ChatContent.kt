package com.drinkit.ai.infra.observation

import io.micrometer.common.KeyValue
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationFilter
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.observation.ChatModelObservationContext
import tools.jackson.databind.json.JsonMapper

/**
 * Puts the messages of a chat call on its span, under the attributes of the GenAI conventions of OpenTelemetry. The
 * prompt is the one the model received, after the guardrails masked its personal data. A photo stays out: its bytes
 * would outweigh the rest of the trace.
 */
internal class ChatContent(private val json: JsonMapper) : ObservationFilter {

    override fun map(context: Observation.Context): Observation.Context {
        if (context !is ChatModelObservationContext) {
            return context
        }

        val (system, conversation) = context.request.instructions.partition { it is SystemMessage }
        if (system.isNotEmpty()) {
            context.add(SYSTEM_INSTRUCTIONS, system.map { textPart(it.text) })
        }
        context.add(INPUT_MESSAGES, conversation.map { message(it) })

        context.response?.let { response ->
            context.add(
                OUTPUT_MESSAGES,
                response.results.map { generation ->
                    val finishReason = generation.metadata.finishReason?.lowercase()
                    message(generation.output) + listOfNotNull(finishReason?.let { "finish_reason" to it })
                },
            )
        }

        return context
    }

    private fun Observation.Context.add(key: String, value: Any) {
        addHighCardinalityKeyValue(KeyValue.of(key, json.writeValueAsString(value)))
    }

    private fun message(message: Message) =
        mapOf("role" to message.messageType.value, "parts" to listOf(textPart(message.text)))

    private fun textPart(text: String?) = mapOf("type" to "text", "content" to text.orEmpty())

    private companion object {
        const val SYSTEM_INSTRUCTIONS = "gen_ai.system_instructions"
        const val INPUT_MESSAGES = "gen_ai.input.messages"
        const val OUTPUT_MESSAGES = "gen_ai.output.messages"
    }
}
