package com.drinkit.rate.limit.infra

import com.drinkit.rate.limit.RateLimited
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.rate.limit.infra.gcra.GcraRateLimiter
import com.drinkit.rate.limit.infra.gcra.TheoreticalArrivals
import com.drinkit.rate.limit.infra.gcra.jooq.JooqTheoreticalArrivals
import org.jooq.DSLContext
import org.springframework.aop.Advisor
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Role
import org.springframework.core.Ordered
import java.time.Clock

@AutoConfiguration(after = [JooqAutoConfiguration::class])
@ConditionalOnBean(DSLContext::class)
@ConditionalOnProperty(prefix = "starters.rate-limit", name = ["enabled"], matchIfMissing = true)
internal class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun theoreticalArrivals(dsl: DSLContext): TheoreticalArrivals = JooqTheoreticalArrivals(dsl)

    @Bean
    @ConditionalOnMissingBean
    fun rateLimiter(theoreticalArrivals: TheoreticalArrivals, clock: Clock): RateLimiter =
        GcraRateLimiter(theoreticalArrivals, clock)

    companion object {

        // Infrastructure, or the advisor creator Spring registers without AspectJ ignores it. Before the transaction
        // of the method, so that a refused call opens none and a counted one takes no second connection
        @Bean
        @JvmStatic
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @ConditionalOnMissingBean(name = ["rateLimitedAdvisor"])
        fun rateLimitedAdvisor(beans: BeanFactory): Advisor = DefaultPointcutAdvisor(
            AnnotationMatchingPointcut(null, RateLimited::class.java, true),
            RateLimitedInterceptor(beans),
        ).apply { order = Ordered.HIGHEST_PRECEDENCE }
    }
}
