package com.drinkit.security.infra.login

import com.drinkit.security.CurrentUser
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.events.SecurityEventLogger
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
internal class UserAccountsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun securityContextHolderStrategy(): SecurityContextHolderStrategy =
        SecurityContextHolder.getContextHolderStrategy()

    @Bean
    @ConditionalOnMissingBean
    fun currentUser(
        holder: SecurityContextHolderStrategy,
        userAccounts: UserAccounts,
        sessionAuthenticationStrategy: SessionAuthenticationStrategy,
        securityContextRepository: SecurityContextRepository,
        securityEventLogger: SecurityEventLogger,
    ): CurrentUser = SpringCurrentUser(
        holder,
        userAccounts,
        sessionAuthenticationStrategy,
        securityContextRepository,
        securityEventLogger,
    )
}
