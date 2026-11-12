package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.math.sqrt

/**
 * Embeds a query and two bottles with a real model, and logs how close the query lands to each. Runs only with
 * `AI_EVALUATION_BASE_URL`, such as `http://localhost:8083/v1` for the LocalAI of compose.
 */
@EnabledIfEnvironmentVariable(named = "AI_EVALUATION_BASE_URL", matches = ".+")
internal class LocalEmbeddingsEvaluationTest {

    private val logger = KotlinLogging.logger {}

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AiAutoConfiguration::class.java))
        .withPropertyValues(
            "starters.ai.endpoints.localai.base-url=${System.getenv("AI_EVALUATION_BASE_URL")}",
            "starters.ai.endpoints.localai.api-key-required=false",
            "starters.ai.endpoints.localai.timeout=300s",
            "starters.ai.models.[$MODEL].endpoint=localai",
            "starters.ai.models.[$MODEL].dimensions=1024",
        )

    @Test
    fun `finds the bottle closest in meaning to a query that shares none of its words`() {
        runner.run { context ->
            // Given
            val embeddings = context.getBean<AiModels>().embeddings(MODEL)
            val (pinotNoir, sauternes) = embeddings.embed(listOf(PINOT_NOIR, SAUTERNES))

            // When
            val query = embeddings.embed("un rouge léger pour accompagner un poisson", INSTRUCTION)

            // Then
            val toPinotNoir = cosine(query, pinotNoir)
            val toSauternes = cosine(query, sauternes)
            logger.info {
                "${query.size} dimensions, cosine to the Pinot Noir $toPinotNoir, to the Sauternes $toSauternes"
            }
            toPinotNoir shouldBeGreaterThan toSauternes
            query.size shouldBe 1024
        }
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val dot = a.indices.sumOf { (a[it] * b[it]).toDouble() }
        val norms = sqrt(a.sumOf { (it * it).toDouble() }) * sqrt(b.sumOf { (it * it).toDouble() })

        return (dot / norms).toFloat()
    }

    private companion object {
        const val MODEL = "qwen3-embedding-0.6b"
        const val INSTRUCTION =
            "Instruct: Given a question about wines, spirits or cocktails, retrieve the matching bottles\nQuery: "
        const val PINOT_NOIR =
            "Domaine des Croix, Bourgogne Pinot Noir 2021. Robe rubis claire, cerise et framboise, tanins souples, se sert frais."
        const val SAUTERNES =
            "Château Guiraud, Sauternes 2015. Liquoreux de Bordeaux, sémillon et sauvignon, miel et abricot confit, avec le foie gras."
    }
}
