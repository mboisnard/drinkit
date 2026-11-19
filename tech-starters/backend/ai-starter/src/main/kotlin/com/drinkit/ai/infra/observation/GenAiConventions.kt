package com.drinkit.ai.infra.observation

import io.micrometer.common.KeyValue
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationFilter
import org.springframework.ai.chat.observation.ChatModelObservationContext
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext
import org.springframework.ai.model.tool.StructuredOutputChatOptions

/**
 * Completes the observations of Spring AI with the GenAI conventions of OpenTelemetry: an embedding call is the
 * `embeddings` operation, where Spring AI writes `embedding`, and a call that hands a JSON schema to the provider asks
 * for a `json` output.
 */
internal class GenAiConventions : ObservationFilter {

    override fun map(context: Observation.Context): Observation.Context {
        if (context is EmbeddingModelObservationContext) {
            context.addLowCardinalityKeyValue(KeyValue.of("gen_ai.operation.name", "embeddings"))
        }

        val options = (context as? ChatModelObservationContext)?.request?.options as? StructuredOutputChatOptions
        if (options?.outputSchema != null) {
            context.addLowCardinalityKeyValue(KeyValue.of("gen_ai.output.type", "json"))
        }

        return context
    }
}
