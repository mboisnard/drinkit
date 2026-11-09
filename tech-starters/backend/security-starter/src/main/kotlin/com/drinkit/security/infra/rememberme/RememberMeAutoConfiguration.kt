package com.drinkit.security.infra.rememberme

import com.drinkit.security.Sessions
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.login.UserAccountsUserDetailsService
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.session.FindByIndexNameSessionRepository
import java.time.Clock

/** Needs the sessions in PostgreSQL: without them, there is no remember-me login. */
@AutoConfiguration(after = [JooqAutoConfiguration::class, JdbcSessionAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
internal class RememberMeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DSLContext::class)
    fun rememberMeTokens(dsl: DSLContext) = RememberMeTokens(dsl)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(FindByIndexNameSessionRepository::class)
    @ConditionalOnSingleCandidate(DSLContext::class)
    @Suppress("LongParameterList")
    fun rememberMeServices(
        userAccounts: UserAccounts,
        rememberMeTokens: RememberMeTokens,
        sessions: Sessions,
        events: SecurityEventLogger,
        clock: Clock,
        properties: SecurityStarterProperties,
    ): RememberMeServices = RotatingRememberMeServices(
        UserAccountsUserDetailsService(userAccounts),
        rememberMeTokens,
        sessions,
        events,
        clock,
        properties.rememberMe.validity,
        properties.cookie,
    )
}
