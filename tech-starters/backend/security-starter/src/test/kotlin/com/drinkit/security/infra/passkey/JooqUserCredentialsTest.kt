package com.drinkit.security.infra.passkey

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.generated.jooq.DrinkitSecurity
import com.drinkit.security.infra.events.SecurityEventLogger
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.UserCredentialRepository

@JooqIntegrationTest(schemas = [DrinkitSecurity::class])
@ExtendWith(OutputCaptureExtension::class)
internal class JooqUserCredentialsTest : UserCredentialsTestContract() {

    override lateinit var credentials: UserCredentialRepository

    @BeforeEach
    fun setup(dsl: DSLContext) {
        credentials = JooqUserCredentials(dsl, SecurityEventLogger())
        JooqUserEntities(dsl).save(
            ImmutablePublicKeyCredentialUserEntity.builder().id(user).name("user-1").displayName("user-1").build(),
        )
    }

    @Test
    fun `logs a passkey added once, not each time a sign-in saves it again`(output: CapturedOutput) {
        // Given
        val passkey = aPasskey()

        // When
        credentials.save(passkey)
        credentials.save(ImmutableCredentialRecord.fromCredentialRecord(passkey).signatureCount(4).build())

        // Then
        output.out shouldContainOnlyOnce "authn_token_created:user-1,type=passkey"
    }

    @Test
    fun `logs a passkey removed`(output: CapturedOutput) {
        // Given
        val passkey = aPasskey().also(credentials::save)

        // When
        credentials.delete(passkey.credentialId)

        // Then
        output.out shouldContainOnlyOnce "authn_token_revoked:user-1,type=passkey"
    }
}
