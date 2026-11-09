package com.drinkit.featuremanagement.infra

import com.drinkit.security.EndpointRules
import com.drinkit.security.endpointRules
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

internal class FeatureManagementSecurityAutoConfigurationTest {

    private val runner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FeatureManagementSecurityAutoConfiguration::class.java))

    @Test
    fun `brings the access rules of its endpoints`() {
        runner.run { context ->
            // When & Then
            context.getBeansOfType(EndpointRules::class.java).keys shouldBe setOf("featureManagementAccessRules")
        }
    }

    @Test
    fun `backs off when the application declares the rules of these endpoints itself`() {
        // Given
        val ownRules = endpointRules { }
        val withOwnRules = runner.withBean("featureManagementAccessRules", EndpointRules::class.java, { ownRules })

        withOwnRules.run { context ->
            // When & Then
            context.getBean("featureManagementAccessRules") shouldBe ownRules
        }
    }
}
