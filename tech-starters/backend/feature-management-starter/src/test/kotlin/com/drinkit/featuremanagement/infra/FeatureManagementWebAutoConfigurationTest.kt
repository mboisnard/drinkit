package com.drinkit.featuremanagement.infra

import com.drinkit.security.CurrentUser
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.InMemoryCurrentUser
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

internal class FeatureManagementWebAutoConfigurationTest {

    private val runner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                FeatureManagementAutoConfiguration::class.java,
                FeatureManagementWebAutoConfiguration::class.java,
            ),
        )
        .withPropertyValues("starters.feature-management.enabled=false")
        .withBean(JsonMapper::class.java, { JsonMapper() })
        .withBean(CurrentUser::class.java, { InMemoryCurrentUser() })
        .withBean(CurrentVisitor::class.java, { CurrentVisitor { "visitor-1" } })

    @Test
    fun `serves the endpoints of flags and experiments even switched off, so that the frontend reads the defaults`() {
        runner.run { context ->
            // When & Then
            context.containsBean("featureManagementEndpoints") shouldBe true
            context.containsBean("requestSubject") shouldBe true
        }
    }

    @Test
    fun `backs off when the application routes the endpoints itself`() {
        // Given
        val ownEndpoints: RouterFunction<ServerResponse> = RouterFunctions.route()
            .GET("/flags") { ServerResponse.ok().build() }
            .build()
        val withOwnEndpoints = runner.withBean(
            "featureManagementEndpoints",
            RouterFunction::class.java,
            { ownEndpoints },
        )

        withOwnEndpoints.run { context ->
            // When & Then
            context.getBean("featureManagementEndpoints") shouldBe ownEndpoints
        }
    }
}
