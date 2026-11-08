package com.drinkit.idempotency.infra.http

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.infra.IdempotencyAutoConfiguration
import com.drinkit.security.CurrentUser
import com.drinkit.security.CurrentVisitor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

// After the filter chain of Spring Security, at -100, which tells the user or the visitor the key belongs to
private const val AFTER_SECURITY = 0

@AutoConfiguration(after = [IdempotencyAutoConfiguration::class])
@ConditionalOnClass(CurrentUser::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(Idempotency::class)
internal class IdempotencyWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["idempotencyFilter"])
    fun idempotencyFilter(
        idempotency: Idempotency,
        transactions: PlatformTransactionManager,
        @Qualifier("requestMappingHandlerMapping") handlerMapping: RequestMappingHandlerMapping,
        currentUser: CurrentUser,
        currentVisitor: CurrentVisitor,
    ): FilterRegistrationBean<IdempotencyFilter> {
        val filter = IdempotencyFilter(
            idempotency,
            TransactionTemplate(transactions),
            handlerMapping,
            currentUser,
            currentVisitor,
        )

        return FilterRegistrationBean(filter).apply { order = AFTER_SECURITY }
    }
}
