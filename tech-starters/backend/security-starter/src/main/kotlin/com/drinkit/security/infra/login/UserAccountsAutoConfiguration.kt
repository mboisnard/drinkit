package com.drinkit.security.infra.login

import com.drinkit.security.CurrentUser
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.events.SecurityEventLogger
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import java.util.concurrent.Semaphore

private const val ARGON2 = "argon2"

// The minimum of the OWASP Password Storage cheat sheet for Argon2id: 19 MiB of memory, 2 iterations, 1 thread
// One hash per processor: a hash keeps a processor busy, so more of them at once would only take more memory
@Suppress("MagicNumber")
private fun boundedArgon2() = BoundedPasswordEncoder(
    Argon2PasswordEncoder(16, 32, 1, 19_456, 2),
    Semaphore(Runtime.getRuntime().availableProcessors()),
)

/**
 * Wires the accounts the application gives through [UserAccounts] into Spring Security, and the [CurrentUser] the
 * application reads. Passwords are hashed with Argon2id at the OWASP minimum, as many at once as there are
 * processors. The `{argon2}` prefix stays on each hash so that a later change of algorithm can still read it.
 * Without it, Spring Boot would create an in-memory user with a generated password.
 *
 * OWASP: ASVS 5.0 V11.4.2, Password Storage Cheat Sheet (Argon2id).
 */
@AutoConfiguration(before = [UserDetailsServiceAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
internal class UserAccountsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder = DelegatingPasswordEncoder(ARGON2, mapOf(ARGON2 to boundedArgon2())).apply {
        // A hash without the {argon2} prefix fails as a wrong password would, not with a 500 that tells it apart
        setDefaultPasswordEncoderForMatches(NoMatchPasswordEncoder)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userDetailsService(userAccounts: UserAccounts): UserDetailsService =
        UserAccountsUserDetailsService(userAccounts)

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

private object NoMatchPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String = error("Passwords are only encoded with Argon2id")

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?) = false
}
