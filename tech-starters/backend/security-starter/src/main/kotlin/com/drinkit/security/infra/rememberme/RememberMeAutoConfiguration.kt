package com.drinkit.security.infra.rememberme

import com.drinkit.security.Sessions
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.login.UserAccountsUserDetailsService
import org.jooq.DSLContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import javax.sql.DataSource

/**
 * Wires the remember-me login, which signs a returning user in from a long-lived cookie once their session has
 * ended. Its tokens live in PostgreSQL beside the sessions, so that every instance of the application sees the same
 * ones and revoking a user's sessions also forgets their remembered browsers. It needs the sessions in PostgreSQL:
 * without them, there is no remember-me login.
 *
 * OWASP: ASVS 5.0 V7.4.1, Session Management Cheat Sheet (server-side token storage).
 */
@AutoConfiguration(after = [JdbcSessionAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
internal class RememberMeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DataSource::class)
    fun rememberMeTokens(dsl: DSLContext, transactionManager: PlatformTransactionManager) =
        RememberMeTokens(dsl, TransactionTemplate(transactionManager))

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(FindByIndexNameSessionRepository::class)
    @ConditionalOnSingleCandidate(DataSource::class)
    @Suppress("LongParameterList")
    fun rememberMeServices(
        userAccounts: UserAccounts,
        rememberMeTokens: RememberMeTokens,
        sessions: Sessions,
        events: SecurityEventLogger,
        clock: ObjectProvider<Clock>,
        properties: SecurityStarterProperties,
    ): RememberMeServices = RotatingRememberMeServices(
        UserAccountsUserDetailsService(userAccounts),
        rememberMeTokens,
        sessions,
        events,
        clock.getIfUnique { Clock.systemUTC() },
        properties.rememberMe.validity,
        properties.cookie,
    )
}
