package com.drinkit.security.infra.code

import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.SignInCodeSender
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.login.NoPasswordSignIn
import com.drinkit.security.infra.login.SignIns
import org.jooq.DSLContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

/** Registers before Spring Boot's in-memory user, which backs off before [NoPasswordSignIn]. */
@AutoConfiguration(
    after = [JooqAutoConfiguration::class, JdbcSessionAutoConfiguration::class],
    before = [UserDetailsServiceAutoConfiguration::class],
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
internal class SignInCodeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DSLContext::class)
    fun signInCodes(dsl: DSLContext): SignInCodes = JooqSignInCodes(dsl)

    @Bean
    @ConditionalOnMissingBean
    fun signInCodeIssuer(
        codes: SignInCodes,
        sender: SignInCodeSender,
        clock: Clock,
        properties: SecurityStarterProperties,
    ) = SignInCodeIssuer(codes, sender, clock, properties.code)

    @Bean
    @ConditionalOnMissingBean
    fun noPasswordSignIn(): AuthenticationProvider = NoPasswordSignIn()

    @Bean
    @ConditionalOnMissingBean
    fun signInCodeLimits(rateLimiter: ObjectProvider<RateLimiter>) = SignInCodeLimits(rateLimiter.ifAvailable)

    @Bean
    @ConditionalOnMissingBean(name = ["signInCodeEndpoint"])
    @Suppress("LongParameterList")
    fun signInCodeEndpoint(
        issuer: SignInCodeIssuer,
        codes: SignInCodes,
        accounts: UserAccounts,
        signIns: SignIns,
        rememberMe: ObjectProvider<RememberMeServices>,
        limits: SignInCodeLimits,
        visitor: CurrentVisitor,
        events: SecurityEventLogger,
        clock: Clock,
        jsonMapper: JsonMapper,
        properties: SecurityStarterProperties,
    ): RouterFunction<ServerResponse> {
        val api = SignInCodeApi(
            issuer,
            codes,
            accounts,
            signIns,
            rememberMe.ifAvailable,
            limits,
            visitor,
            events,
            clock,
            jsonMapper,
            properties.code.maxEmailLength,
        )

        return RouterFunctions.route()
            .POST(CODE_REQUEST_PATH, api::requestCode)
            .POST(CODE_SIGN_IN_PATH, api::signIn)
            .build()
    }
}
