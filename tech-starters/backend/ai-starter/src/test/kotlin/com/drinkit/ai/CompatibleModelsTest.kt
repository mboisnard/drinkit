package com.drinkit.ai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class CompatibleModelsTest {

    private val aiModels = FakeAiModels()
    private val compatibleModels = CompatibleModels(aiModels)

    @Test
    fun `answers with the first model of the list`() {
        // Given
        aiModels.model("claude-sonnet").answers("Bordeaux")
        aiModels.model("qwen3-vl-4b").answers("Bourgogne")

        // When
        val answer = askTheRegion()

        // Then
        answer shouldBe ModelAnswer("claude-sonnet", "Bordeaux")
    }

    @Test
    fun `moves on to the next model when a model fails`() {
        // Given
        aiModels.model("claude-sonnet").fails(IllegalStateException("Overloaded"))
        aiModels.model("qwen3-vl-4b").answers("Bourgogne")

        // When
        val answer = askTheRegion()

        // Then
        answer shouldBe ModelAnswer("qwen3-vl-4b", "Bourgogne")
    }

    @Test
    fun `skips an unavailable model`() {
        // Given
        aiModels.model("qwen3-vl-4b").answers("Bourgogne")

        // When
        val answer = askTheRegion()

        // Then
        answer shouldBe ModelAnswer("qwen3-vl-4b", "Bourgogne")
    }

    @Test
    fun `fails with the cause of each model when none answers`() {
        // Given
        val overloaded = IllegalStateException("Overloaded")
        aiModels.model("qwen3-vl-4b").fails(overloaded)

        // When
        val failure = shouldThrow<AllModelsFailedException> { askTheRegion() }

        // Then
        failure.causes.keys shouldBe setOf("claude-sonnet", "qwen3-vl-4b")
        failure.causes.getValue("claude-sonnet").shouldBeInstanceOf<ModelUnavailableException>()
        failure.causes.getValue("qwen3-vl-4b") shouldBe overloaded
    }

    @Test
    fun `keeps the errors of the providers out of the message of the failure`() {
        // Given
        aiModels.model("qwen3-vl-4b").fails(IllegalStateException("401: invalid x-api-key"))

        // When
        val failure = shouldThrow<AllModelsFailedException> { askTheRegion() }

        // Then
        failure.message shouldBe "No model answered among claude-sonnet, qwen3-vl-4b"
    }

    @Test
    fun `asks a model again when its typed answer does not match the type`() {
        // Given
        val model = aiModels.model("qwen3-vl-4b")
            .answers(
                """{"producer": "Château Les Trois Chênes"}""",
                """{"producer": "Château Les Trois Chênes", "vintage": 2019}""",
            )

        // When
        val answer = compatibleModels.entity(listOf("qwen3-vl-4b"), Label::class) { it.prompt().user("Read the label") }

        // Then
        answer.value shouldBe Label("Château Les Trois Chênes", 2019)
        model.prompts shouldHaveSize 2
    }

    @Test
    fun `fails naming the field of a typed answer that stays invalid`() {
        // Given
        val invalid = """{"producer": "Château Les Trois Chênes", "vintage": "two thousand nineteen"}"""
        aiModels.model("qwen3-vl-4b").answers(invalid, invalid, invalid, invalid)

        // When
        val failure = shouldThrow<AllModelsFailedException> {
            compatibleModels.entity(listOf("qwen3-vl-4b"), Label::class) { it.prompt().user("Read the label") }
        }

        // Then
        failure.causes.getValue("qwen3-vl-4b").shouldBeInstanceOf<InvalidAnswerException>().field shouldBe "vintage"
    }

    private fun askTheRegion() =
        compatibleModels.call(listOf("claude-sonnet", "qwen3-vl-4b")) { it.prompt().user("Region?").call().content() }

    private data class Label(val producer: String, val vintage: Int)
}
