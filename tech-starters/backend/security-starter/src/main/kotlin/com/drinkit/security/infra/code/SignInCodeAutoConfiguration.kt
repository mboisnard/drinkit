package com.drinkit.security.infra.code

import com.drinkit.security.SignInCodeSender
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityStarterProperties
import org.jooq.DSLContext
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
    @ConditionalOnMissingBean(name = ["signInCodeEndpoint"])
    fun signInCodeEndpoint(
        issuer: SignInCodeIssuer,
        jsonMapper: JsonMapper,
        properties: SecurityStarterProperties,
    ): RouterFunction<ServerResponse> {
        val endpoint = SignInCodeEndpoint(issuer, jsonMapper, properties.code.maxEmailLength)

        return RouterFunctions.route().POST(CODE_REQUEST_PATH, endpoint::requestCode).build()
    }
}
