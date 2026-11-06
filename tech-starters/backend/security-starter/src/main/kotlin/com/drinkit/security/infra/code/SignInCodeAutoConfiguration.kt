package com.drinkit.security.infra.code

import com.drinkit.security.SignInCodeSender
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityStarterProperties
import org.jooq.DSLContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import javax.sql.DataSource

/**
 * Wires the sign-in by a code sent by email: the request for a code, its check, and the codes in PostgreSQL, which
 * the application needs. The application gives the [SignInCodeSender] that sends a code, and the [UserAccounts] that
 * find or create the account of a proven login. It registers before Spring Boot's in-memory user, which then backs
 * off.
 *
 * OWASP: ASVS 5.0 V6.6, Authentication Cheat Sheet (out-of-band codes).
 */
@AutoConfiguration(after = [JdbcSessionAutoConfiguration::class], before = [UserDetailsServiceAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
internal class SignInCodeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DataSource::class)
    fun signInCodes(dsl: DSLContext, transactionManager: PlatformTransactionManager): SignInCodes =
        JooqSignInCodes(dsl, TransactionTemplate(transactionManager))

    @Bean
    @ConditionalOnMissingBean
    fun signInCodeIssuer(
        codes: SignInCodes,
        sender: SignInCodeSender,
        clock: ObjectProvider<Clock>,
        properties: SecurityStarterProperties,
    ) = SignInCodeIssuer(codes, sender, clock.getIfUnique { Clock.systemUTC() }, properties.code)

    @Bean
    @ConditionalOnMissingBean
    fun emailCodeAuthenticationProvider(codes: SignInCodes, accounts: UserAccounts, clock: ObjectProvider<Clock>) =
        EmailCodeAuthenticationProvider(codes, accounts, clock.getIfUnique { Clock.systemUTC() })

    @Bean
    @ConditionalOnMissingBean(name = ["signInCodeEndpoint"])
    fun signInCodeEndpoint(
        issuer: SignInCodeIssuer,
        jsonMapper: JsonMapper,
        properties: SecurityStarterProperties,
    ): RouterFunction<ServerResponse> = RouterFunctions.route()
        .POST(CODE_REQUEST_PATH) { request ->
            val servletRequest = request.servletRequest()
            when (val login = servletRequest.readSignInBody(jsonMapper)?.login(properties.code.maxEmailLength)) {
                null -> ServerResponse.badRequest().build()
                else -> ServerResponse.noContent().build().also { issuer.issue(login, servletRequest.locale) }
            }
        }
        .build()
}
