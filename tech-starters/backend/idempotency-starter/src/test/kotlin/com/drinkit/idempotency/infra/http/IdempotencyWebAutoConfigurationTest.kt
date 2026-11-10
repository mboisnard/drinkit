package com.drinkit.idempotency.infra.http

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.inMemoryIdempotency
import com.drinkit.security.CurrentUser
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.InMemoryCurrentUser
import com.drinkit.security.InMemoryCurrentVisitor
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

internal class IdempotencyWebAutoConfigurationTest {

    private val runner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(IdempotencyWebAutoConfiguration::class.java))
        .withBean(PlatformTransactionManager::class.java, { DataSourceTransactionManager(DriverManagerDataSource()) })
        .withBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        .withBean(CurrentUser::class.java, { InMemoryCurrentUser() })
        .withBean(CurrentVisitor::class.java, {
            InMemoryCurrentVisitor("visitor")
        })

    private val withIdempotency = runner.withBean(Idempotency::class.java, { inMemoryIdempotency() })

    @Test
    fun `registers the filter of the idempotent endpoints in a servlet application`() {
        withIdempotency.run { context ->
            // When & Then
            context.containsBean("idempotencyFilter") shouldBe true
        }
    }

    @Test
    fun `backs off when the application declares its own idempotencyFilter`() {
        // Given
        val own = FilterRegistrationBean<IdempotencyFilter>()

        // When & Then
        withIdempotency.withBean("idempotencyFilter", FilterRegistrationBean::class.java, { own })
            .run { context -> context.getBean("idempotencyFilter") shouldBeSameInstanceAs own }
    }

    @Test
    fun `registers nothing without an Idempotency bean`() {
        runner.run { context ->
            // When & Then
            context.containsBean("idempotencyFilter") shouldBe false
        }
    }
}
