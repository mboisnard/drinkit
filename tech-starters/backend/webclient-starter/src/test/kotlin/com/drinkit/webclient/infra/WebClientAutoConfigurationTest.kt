package com.drinkit.webclient.infra

import com.drinkit.idempotency.CurrentIdempotencyKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.registry.ImportHttpServices
import java.io.IOException
import java.net.URI

internal class WebClientAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                HttpClientAutoConfiguration::class.java,
                HttpServiceClientPropertiesAutoConfiguration::class.java,
                RestClientAutoConfiguration::class.java,
                HttpServiceClientAutoConfiguration::class.java,
                WebClientAutoConfiguration::class.java,
            ),
        )
        .withUserConfiguration(GreetingClients::class.java)
        .withPropertyValues(
            "spring.http.serviceclient.greeting.base-url=http://greeting.test",
            "starters.webclient.retry.delay=0",
        )

    @Test
    fun `a GET that failed on an I-O error is sent again`() {
        runner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 2) })
            .run { context ->
                // When
                val greeting = context.getBean<GreetingClient>().greeting()

                // Then
                greeting shouldBe GREETING
                context.getBean<FlakyServer>().attempts shouldBe 3
            }
    }

    @Test
    fun `a POST is sent once, even when it fails`() {
        runner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 1) })
            .run { context ->
                // When
                shouldThrow<ResourceAccessException> { context.getBean<GreetingClient>().greet() }

                // Then
                context.getBean<FlakyServer>().attempts shouldBe 1
            }
    }

    @Test
    fun `once its retries are spent, a GET gives the caller every I-O failure, the last one first`() {
        runner.withPropertyValues("starters.webclient.retry.max-retries=1")
            .withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 10) })
            .run { context ->
                // When
                val failure = shouldThrow<ResourceAccessException> { context.getBean<GreetingClient>().greeting() }

                // Then
                failure.cause?.message shouldBe "$CONNECTION_RESET 2"
                failure.cause?.suppressed?.map { it.message } shouldBe listOf("$CONNECTION_RESET 1")
            }
    }

    @Test
    fun `the application's own retry configurer replaces the starter's`() {
        runner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 1) })
            .withBean(
                "retryingHttpServiceGroupConfigurer",
                RestClientHttpServiceGroupConfigurer::class.java,
                { RestClientHttpServiceGroupConfigurer { } },
            )
            .run { context ->
                // When
                shouldThrow<ResourceAccessException> { context.getBean<GreetingClient>().greeting() }

                // Then
                context.getBean<FlakyServer>().attempts shouldBe 1
            }
    }

    @Test
    fun `a GET fails at once when the starter is switched off`() {
        runner.withPropertyValues("starters.webclient.enabled=false")
            .withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 1) })
            .run { context ->
                // When
                shouldThrow<ResourceAccessException> { context.getBean<GreetingClient>().greeting() }

                // Then
                context.getBean<FlakyServer>().attempts shouldBe 1
            }
    }

    @Nested
    inner class `Calling a provider` {

        private val idempotentRunner = runner.withPropertyValues("starters.webclient.retry.idempotent-groups=greeting")

        @Test
        fun `a provider call carrying a key is retried after a network failure`() {
            idempotentRunner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 1) })
                .run { context ->
                    // When
                    context.getBean<GreetingClient>().greet(key = "greeting-1")

                    // Then
                    context.getBean<FlakyServer>().keys shouldBe listOf("greeting-1", "greeting-1")
                }
        }

        @Test
        fun `a provider call without a key is never retried`() {
            idempotentRunner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 1) })
                .run { context ->
                    // When
                    shouldThrow<ResourceAccessException> { context.getBean<GreetingClient>().greet() }

                    // Then
                    context.getBean<FlakyServer>().attempts shouldBe 1
                }
        }

        @Test
        fun `a provider call made for an event carries a key derived from the event's, kept on a replay`() {
            idempotentRunner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 0) })
                .run { context ->
                    // When
                    repeat(
                        2,
                    ) { CurrentIdempotencyKey.with("welcome-email") { context.getBean<GreetingClient>().greet() } }

                    // Then
                    val (first, replay) = context.getBean<FlakyServer>().keys
                    first shouldNotBe null
                    replay shouldBe first
                }
        }

        @Test
        fun `a call carrying a key to a provider that does not honour keys is sent once`() {
            runner.withBean(FlakyServer::class.java, { FlakyServer(failuresBeforeSuccess = 1) })
                .run { context ->
                    // When
                    shouldThrow<ResourceAccessException> { context.getBean<GreetingClient>().greet(key = "greeting-1") }

                    // Then
                    context.getBean<FlakyServer>().attempts shouldBe 1
                }
        }
    }

    interface GreetingClient {
        @GetExchange("/greeting")
        fun greeting(): String

        @PostExchange("/greeting")
        fun greet(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER, required = false) key: String? = null,
        ): String
    }

    // A configurer of the application's own, as the clients of a module declare one for their group
    @Configuration(proxyBeanMethods = false)
    @ImportHttpServices(group = "greeting", types = [GreetingClient::class])
    class GreetingClients {

        @Bean
        fun flakyServerHttpServiceGroupConfigurer(server: FlakyServer) =
            RestClientHttpServiceGroupConfigurer { groups ->
                groups.forEachClient { _, clientBuilder -> clientBuilder.requestFactory(server) }
            }
    }

    class FlakyServer(private val failuresBeforeSuccess: Int) : ClientHttpRequestFactory {
        var attempts = 0
            private set
        val keys = mutableListOf<String?>()

        override fun createRequest(uri: URI, httpMethod: HttpMethod): ClientHttpRequest =
            object : MockClientHttpRequest(httpMethod, uri) {
                override fun executeInternal(): ClientHttpResponse {
                    attempts++
                    keys += headers.getFirst(IDEMPOTENCY_KEY_HEADER)
                    if (attempts <= failuresBeforeSuccess) {
                        throw IOException("$CONNECTION_RESET $attempts")
                    }
                    return MockClientHttpResponse(GREETING.toByteArray(), HttpStatus.OK)
                }
            }
    }

    companion object {
        private const val GREETING = "Hello"
        private const val CONNECTION_RESET = "Connection reset"
    }
}
