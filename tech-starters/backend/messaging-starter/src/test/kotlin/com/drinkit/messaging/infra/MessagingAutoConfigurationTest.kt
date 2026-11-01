package com.drinkit.messaging.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.messaging.infra.springevents.SpringEventsPublisher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class MessagingAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration::class.java))

    @Test
    fun `registers the publisher of platform events`() {
        runner.run { context ->
            context.getBean<PlatformEventPublisher>().shouldBeInstanceOf<SpringEventsPublisher>()
        }
    }

    @Test
    fun `backs off when the application declares its own publisher`() {
        runner.withBean(PlatformEventPublisher::class.java, { SpyPlatformEventPublisher() })
            .run { context ->
                context.getBean<PlatformEventPublisher>().shouldBeInstanceOf<SpyPlatformEventPublisher>()
            }
    }

    @Test
    fun `retries with an exponential backoff by default`() {
        runner.run { context ->
            context.getBean<Retry>().shouldBeInstanceOf<ExponentialBackoffRetry>()
        }
    }

    @Test
    fun `retries with a fixed delay when the exponential backoff is switched off`() {
        runner.withPropertyValues("starters.messaging.retry.exponential-backoff=false")
            .run { context ->
                context.getBean<Retry>().shouldBeInstanceOf<FixedDelayRetry>()
            }
    }

    @Test
    fun `registers no publisher when disabled`() {
        runner.withPropertyValues("starters.messaging.enabled=false")
            .run { context ->
                context.getBeanNamesForType(PlatformEventPublisher::class.java).size shouldBe 0
            }
    }
}
