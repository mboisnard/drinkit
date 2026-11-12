package com.drinkit.ai.infra

import com.drinkit.ai.Embeddings
import com.drinkit.ai.infra.guardrails.patterns.PersonalData
import org.springframework.ai.embedding.EmbeddingModel

/** Masks the personal data of each text, as in a prompt, and checks the vectors the model returns. */
internal class DefaultEmbeddings(
    private val model: String,
    private val embeddingModel: EmbeddingModel,
    override val dimensions: Int,
) : Embeddings {

    override fun embed(texts: List<String>, instruction: String): List<FloatArray> {
        if (texts.isEmpty()) return emptyList()

        val vectors = embeddingModel.embed(texts.map { instruction + PersonalData.mask(it) })

        check(vectors.size == texts.size) { "Model $model returned ${vectors.size} vectors for ${texts.size} texts" }
        vectors.forEach { vector ->
            check(vector.size == dimensions) {
                "Model $model returned a vector of ${vector.size} dimensions, its catalog declares $dimensions"
            }
        }

        return vectors
    }
}
