package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import com.drinkit.ai.AiModelsTestContract
import com.drinkit.ai.infra.AiProperties.Endpoint
import com.drinkit.ai.infra.AiProperties.Model
import com.drinkit.ai.infra.AiProperties.Provider
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class DefaultAiModelsTest : AiModelsTestContract() {

    private val server = FakeModelServer()
    private val localAi = Endpoint(baseUrl = "${server.url}/v1", apiKeyRequired = false)
    private val anthropic = Endpoint(provider = Provider.ANTHROPIC, baseUrl = server.url, apiKey = "sk-ant-test")
    private val catalog = mutableMapOf<String, Model>()

    override fun fetchAiModels(): AiModels = DefaultAiModels(properties(), ObservationRegistry.NOOP)

    override fun givenModel(model: String, answer: String) {
        catalog[model] = Model(endpoint = "localai")
        server.answers(model, answer)
    }

    override fun givenEmbeddingModel(model: String) {
        catalog[model] = Model(endpoint = "localai", dimensions = 3)
    }

    @AfterEach
    fun stopServer() = server.close()

    @Test
    fun `models on the Anthropic and the OpenAI protocols answer from the same configuration`() {
        // Given
        catalog["claude-sonnet"] = Model(endpoint = "anthropic", name = "claude-sonnet-5-5")
        catalog["qwen3-vl-4b"] = Model(endpoint = "localai", name = "qwen3-vl-4b-instruct")
        server.answers("claude-sonnet-5-5", "Bordeaux")
        server.answers("qwen3-vl-4b-instruct", "Bourgogne")
        val aiModels = DefaultAiModels(properties(), ObservationRegistry.NOOP)

        // When
        val answers = listOf("claude-sonnet", "qwen3-vl-4b").map { aiModels.ask(it) }

        // Then
        answers shouldBe listOf("Bordeaux", "Bourgogne")
    }

    @Test
    fun `observes each call with its model, its duration and its tokens, never with the prompt`() {
        // Given
        givenModel("qwen3-vl-4b", answer = "Bourgogne")
        val observations = RecordedObservations()
        val registry = ObservationRegistry.create().apply { observationConfig().observationHandler(observations) }

        // When
        DefaultAiModels(properties(), registry).ask("qwen3-vl-4b", question = "Which region is Pommard in?")

        // Then
        val call = observations.stopped.single { it.name == "gen_ai.client.operation" }
        call.started shouldBe true
        call.keys shouldContain ("gen_ai.request.model" to "qwen3-vl-4b")
        call.keys shouldContain ("gen_ai.usage.input_tokens" to "${FakeModelServer.PROMPT_TOKENS}")
        call.keys shouldContain ("gen_ai.usage.output_tokens" to "${FakeModelServer.ANSWER_TOKENS}")
        observations.stopped.flatMap { it.keys }.forEach { (_, value) -> value shouldNotContain "Pommard" }
    }

    @Test
    fun `observes an embedding with its model and its tokens, never with its text`() {
        // Given
        givenEmbeddingModel("qwen3-embedding-0.6b")
        server.embeds("qwen3-embedding-0.6b", "Pommard 2019", floatArrayOf(0.1f, 0.2f, 0.3f))
        val observations = RecordedObservations()
        val registry = ObservationRegistry.create().apply { observationConfig().observationHandler(observations) }

        // When
        DefaultAiModels(properties(), registry).embeddings("qwen3-embedding-0.6b").embed("Pommard 2019")

        // Then
        val call = observations.stopped.single { it.name == "gen_ai.client.operation" }
        call.keys shouldContain ("gen_ai.request.model" to "qwen3-embedding-0.6b")
        call.keys shouldContain ("gen_ai.usage.input_tokens" to "${FakeModelServer.PROMPT_TOKENS}")
        observations.stopped.flatMap { it.keys }.forEach { (_, value) -> value shouldNotContain "Pommard" }
    }

    private fun properties() =
        AiProperties(endpoints = mapOf("localai" to localAi, "anthropic" to anthropic), models = catalog.toMap())

    private fun AiModels.ask(model: String, question: String = "Which region?") =
        chatClient(model).prompt().user(question).call().content()
}
