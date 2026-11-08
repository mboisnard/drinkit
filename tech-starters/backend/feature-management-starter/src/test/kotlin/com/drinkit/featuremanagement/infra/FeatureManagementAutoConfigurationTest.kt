package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.Assignment
import com.drinkit.featuremanagement.Experiments
import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.featuremanagement.FlagSubject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class FeatureManagementAutoConfigurationTest {

    private val runner = ApplicationContextRunner().withConfiguration(
        AutoConfigurations.of(
            FeatureManagementAutoConfiguration::class.java,
            FeatureManagementHealthAutoConfiguration::class.java,
        ),
    )

    private val ada = FlagSubject("ada")

    @Test
    fun `answers the default of a flag when Flipt does not answer, and reports Flipt down`() {
        // Given
        runner.withPropertyValues("starters.feature-management.flipt.url=http://localhost:1").run { context ->
            // When
            val enabled = context.getBean<FeatureFlags>().isEnabled("new-checkout", ada, default = true)
            val health = context.getBean<HealthIndicator>("fliptHealthIndicator").health()

            // Then
            enabled shouldBe true
            health?.status shouldBe Status.DOWN
        }
    }

    @Test
    fun `answers the defaults, with no Flipt, when the starter is switched off`() {
        // Given
        runner.withPropertyValues("starters.feature-management.enabled=false").run { context ->
            // When
            val enabled = context.getBean<FeatureFlags>().isEnabled("new-checkout", ada)
            val assignment = context.getBean<Experiments>().assign("checkout", ada)

            // Then
            enabled shouldBe false
            assignment shouldBe Assignment.outOf("checkout", ada)
            context.getBeansOfType<HealthIndicator>() shouldBe emptyMap()
        }
    }

    @Test
    fun `refuses to start without the URL of Flipt`() {
        // When
        runner.run { context ->
            // Then
            generateSequence(context.startupFailure) { it.cause }.last().message.orEmpty() shouldContain
                "starters.feature-management.flipt.url"
        }
    }
}
