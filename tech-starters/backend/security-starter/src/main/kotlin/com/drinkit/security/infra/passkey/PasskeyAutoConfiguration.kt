package com.drinkit.security.infra.passkey

import com.drinkit.security.CurrentUser
import com.drinkit.security.Passkeys
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.login.SignIns
import com.drinkit.security.infra.login.UserAccountsUserDetailsService
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement
import org.springframework.security.web.webauthn.api.UserVerificationRequirement
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations

/**
 * Spring Security's passkeys, with two changes to its options: the passkey manager shows the user's login rather than
 * the account id, and the authenticator must verify the user, so that a passkey is a strong factor of its own.
 *
 * W3C WebAuthn Level 3, 5.4 (creation options), 5.5 (request options) and user-verification. Spring Security,
 * Passkeys. OWASP: ASVS 5.0 6.3.3, Authentication Cheat Sheet (FIDO).
 */
@AutoConfiguration(after = [JooqAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty("starters.security.enabled", matchIfMissing = true)
@ConditionalOnBooleanProperty("starters.security.passkey.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
internal class PasskeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DSLContext::class)
    fun userEntities(dsl: DSLContext): PublicKeyCredentialUserEntityRepository = JooqUserEntities(dsl)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DSLContext::class)
    fun userCredentials(dsl: DSLContext, events: SecurityEventLogger): UserCredentialRepository =
        JooqUserCredentials(dsl, events)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PublicKeyCredentialUserEntityRepository::class, UserCredentialRepository::class)
    fun webAuthnRelyingPartyOperations(
        userEntities: PublicKeyCredentialUserEntityRepository,
        userCredentials: UserCredentialRepository,
        accounts: UserAccounts,
        holder: SecurityContextHolderStrategy,
        properties: SecurityStarterProperties,
    ): WebAuthnRelyingPartyOperations {
        val passkey = properties.passkey.also { it.requireRelyingParty() }
        val rp = PublicKeyCredentialRpEntity.builder().id(passkey.rpId).name(passkey.rpName).build()

        return Webauthn4JRelyingPartyOperations(userEntities, userCredentials, rp, passkey.allowedOrigins).apply {
            setCustomizeCreationOptions { options ->
                val signedIn = requireNotNull(holder.context.authentication) { "Only a signed-in user creates one" }
                val id = signedIn.name
                val login = requireNotNull(accounts.findById(id)) { "No account $id" }.login
                // Spring has just saved the user entity, whose random handle the passkey keeps
                val userEntity = requireNotNull(userEntities.findByUsername(id)) { "No user entity for $id" }

                options
                    .user(
                        ImmutablePublicKeyCredentialUserEntity.builder()
                            .id(userEntity.id)
                            .name(login)
                            .displayName(login)
                            .build(),
                    )
                    .authenticatorSelection(
                        AuthenticatorSelectionCriteria.builder()
                            .residentKey(ResidentKeyRequirement.REQUIRED)
                            .userVerification(UserVerificationRequirement.REQUIRED)
                            .build(),
                    )
            }
            setCustomizeRequestOptions { it.userVerification(UserVerificationRequirement.REQUIRED) }
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WebAuthnRelyingPartyOperations::class)
    fun passkeySignIn(
        accounts: UserAccounts,
        signIns: SignIns,
        sessionAuthenticationStrategy: SessionAuthenticationStrategy,
        securityContextRepository: SecurityContextRepository,
        events: SecurityEventLogger,
    ) = PasskeySignIn(
        UserAccountsUserDetailsService(accounts),
        signIns,
        sessionAuthenticationStrategy,
        securityContextRepository,
        events,
    )

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WebAuthnRelyingPartyOperations::class)
    fun passkeys(
        userEntities: PublicKeyCredentialUserEntityRepository,
        userCredentials: UserCredentialRepository,
        currentUser: CurrentUser,
    ): Passkeys = SpringPasskeys(userEntities, userCredentials, currentUser)
}
