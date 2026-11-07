package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.security.EndpointRules
import com.drinkit.security.endpointRules
import com.drinkit.sse.ServerEvents
import com.drinkit.sse.SpyServerEvents
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.NestedExceptionUtils.getMostSpecificCause

internal class SseAutoConfigurationTest {

    private val runner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                SseAutoConfiguration::class.java,
                SseSecurityAutoConfiguration::class.java,
            ),
        )

    private val withMessaging = runner.withBean(PlatformEventPublisher::class.java, { SpyPlatformEventPublisher() })

    @Test
    fun `registers the server events, their streams and their endpoint`() {
        withMessaging.run { context ->
            context.getBean(ServerEvents::class.java).shouldBeInstanceOf<QueuedServerEvents>()
            context.getBeansOfType(InMemoryStreams::class.java).size shouldBe 1
            context.containsBean("serverEventsEndpoint") shouldBe true
        }
    }

    @Test
    fun `backs off when the application declares its own server events`() {
        val own = SpyServerEvents()

        withMessaging.withBean(ServerEvents::class.java, { own })
            .run { context -> context.getBean(ServerEvents::class.java) shouldBeSameInstanceAs own }
    }

    @Test
    fun `registers nothing when disabled`() {
        withMessaging.withPropertyValues("starters.sse.enabled=false").run { context ->
            context.getBeansOfType(ServerEvents::class.java) shouldBe emptyMap()
            context.containsBean("serverEventsEndpoint") shouldBe false
            context.containsBean("serverEventsAccessRules") shouldBe false
        }
    }

    @Test
    fun `refuses to start without the messaging starter, which carries the events to the streams`() {
        runner.run { context ->
            getMostSpecificCause(context.startupFailure.shouldNotBeNull()).message shouldBe
                "The SSE starter delivers through the messaging starter: keep starters.messaging.enabled"
        }
    }

    @Test
    fun `brings the access rule of its endpoint along with the security starter`() {
        withMessaging.run { context ->
            context.getBean("serverEventsAccessRules").shouldBeInstanceOf<EndpointRules>()
        }
    }

    @Test
    fun `brings no access rule without the security starter`() {
        withMessaging.withClassLoader(FilteredClassLoader(EndpointRules::class.java))
            .run { context -> context.containsBean("serverEventsAccessRules") shouldBe false }
    }

    @Test
    fun `keeps the access rule the application gives its endpoint`() {
        val own = endpointRules { authorize("/sse/**", hasRole("USER")) }

        withMessaging.withBean("serverEventsAccessRules", EndpointRules::class.java, { own })
            .run { context -> context.getBean("serverEventsAccessRules") shouldBeSameInstanceAs own }
    }
}
