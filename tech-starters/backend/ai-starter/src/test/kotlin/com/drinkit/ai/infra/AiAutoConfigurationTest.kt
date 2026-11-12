package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import com.drinkit.ai.FakeAiModels
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource

internal class AiAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AiAutoConfiguration::class.java))
        .withPropertyValues(
            "starters.ai.endpoints.localai.base-url=http://localhost:8083/v1",
            "starters.ai.endpoints.localai.api-key-required=false",
            "starters.ai.endpoints.anthropic.provider=anthropic",
            "starters.ai.models.qwen3-vl-4b.endpoint=localai",
            "starters.ai.models.claude-sonnet-5-5.endpoint=anthropic",
        )

    @Test
    fun `makes the models of the catalog available`() {
        runner.run { context ->
            // When & Then
            context.getBean<AiModels>().isAvailable("qwen3-vl-4b") shouldBe true
        }
    }

    @Test
    fun `starts without the key of a model, which is then unavailable`() {
        runner.run { context ->
            // When & Then
            context.getBean<AiModels>().isAvailable("claude-sonnet-5-5") shouldBe false
        }
    }

    @Test
    fun `reads the key of an endpoint from its environment variable`() {
        // Given
        val withKey = runner.withInitializer { context ->
            context.environment.propertySources.addFirst(
                SystemEnvironmentPropertySource(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    mapOf("STARTERS_AI_ENDPOINTS_ANTHROPIC_APIKEY" to "sk-ant-test"),
                ),
            )
        }

        withKey.run { context ->
            // When & Then
            context.getBean<AiModels>().isAvailable("claude-sonnet-5-5") shouldBe true
        }
    }

    @Test
    fun `applies the chat client customizers of the application to each available model`() {
        // Given
        val customized = mutableListOf<ChatClient.Builder>()
        val withCustomizer = runner
            .withPropertyValues("starters.ai.endpoints.anthropic.api-key=sk-ant-test")
            .withBean(ChatClientBuilderCustomizer::class.java, { ChatClientBuilderCustomizer { customized += it } })

        withCustomizer.run { context ->
            // When
            context.getBean<AiModels>()

            // Then
            customized shouldHaveSize 2
        }
    }

    @Test
    fun `registers the token metrics when the application has a meter registry`() {
        // Given
        val withMetrics = runner
            .withConfiguration(AutoConfigurations.of(AiMetricsAutoConfiguration::class.java))
            .withBean(SimpleMeterRegistry::class.java, { SimpleMeterRegistry() })

        withMetrics.run { context ->
            // When & Then
            context.getBeansOfType(ChatModelMeterObservationHandler::class.java).values shouldHaveSize 1
        }
    }

    @Test
    fun `makes every model unavailable when the starter is switched off`() {
        // Given
        val disabled = runner.withPropertyValues("starters.ai.enabled=false")

        disabled.run { context ->
            // When & Then
            context.getBean<AiModels>().isAvailable("qwen3-vl-4b") shouldBe false
        }
    }

    @Test
    fun `backs off when the application declares its own models`() {
        // Given
        val withOwnModels = runner.withBean(AiModels::class.java, { FakeAiModels() })

        withOwnModels.run { context ->
            // When & Then
            context.getBean<AiModels>().shouldBeInstanceOf<FakeAiModels>()
        }
    }

    @Test
    fun `refuses to start when a model names an endpoint that is not declared`() {
        // Given
        val withUnknownEndpoint = runner.withPropertyValues("starters.ai.models.gpt-5.endpoint=openai")

        withUnknownEndpoint.run { context ->
            // When & Then
            generateSequence(context.startupFailure) { it.cause }.last().message.orEmpty() shouldContain
                "Model gpt-5 names the endpoint openai, which starters.ai.endpoints does not declare"
        }
    }
}
