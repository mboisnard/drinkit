package com.drinkit.ai.infra.observation

import io.micrometer.common.KeyValue
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationFilter
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext

/**
 * Completes the observations of Spring AI with the GenAI conventions of OpenTelemetry: an embedding call is the
 * `embeddings` operation, where Spring AI writes `embedding`.
 */
internal class GenAiConventions : ObservationFilter {

    override fun map(context: Observation.Context): Observation.Context {
        if (context is EmbeddingModelObservationContext) {
            context.addLowCardinalityKeyValue(KeyValue.of("gen_ai.operation.name", "embeddings"))
        }

        return context
    }
}
