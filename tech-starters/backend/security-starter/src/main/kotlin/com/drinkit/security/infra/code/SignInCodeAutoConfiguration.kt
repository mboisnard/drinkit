package com.drinkit.security.infra.code

import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.SignInCodeSender
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityErrorResponses
import com.drinkit.security.infra.SecurityStarterProperties
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
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

/** Registers before Spring Boot's in-memory user, which then backs off. */
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
    fun emailCodeAuthenticationProvider(codes: SignInCodes, accounts: UserAccounts, clock: Clock) =
        EmailCodeAuthenticationProvider(codes, accounts, clock)

    @Bean
    @ConditionalOnMissingBean
    fun signInCodeLimits(rateLimiter: ObjectProvider<RateLimiter>) = SignInCodeLimits(rateLimiter.ifAvailable)

    @Bean
    @ConditionalOnMissingBean(name = ["signInCodeEndpoint"])
    fun signInCodeEndpoint(
        issuer: SignInCodeIssuer,
        limits: SignInCodeLimits,
        visitor: CurrentVisitor,
        jsonMapper: JsonMapper,
        properties: SecurityStarterProperties,
    ): RouterFunction<ServerResponse> {
        val api = SignInCodeApi(
            issuer,
            limits,
            visitor,
            SecurityErrorResponses(jsonMapper),
            jsonMapper,
            properties.code.maxEmailLength,
        )

        return RouterFunctions.route().POST(CODE_REQUEST_PATH, api::requestCode).build()
    }
}
