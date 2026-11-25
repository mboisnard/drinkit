package com.drinkit.security.infra.passkey

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import java.time.Instant

/** The behavior of Spring Security's own repository tests, for the jOOQ one and the in-memory one of Spring. */
internal abstract class UserCredentialsTestContract {

    abstract val credentials: UserCredentialRepository

    protected val user: Bytes = Bytes.random()

    @Test
    fun `finds a saved passkey by its id and by its user`() {
        // Given
        val passkey = aPasskey()

        // When
        credentials.save(passkey)

        // Then
        credentials.findByCredentialId(passkey.credentialId).shouldNotBeNull().fields() shouldBe passkey.fields()
        credentials.findByUserId(user).map { it.fields() } shouldBe listOf(passkey.fields())
    }

    @Test
    fun `finds nothing for an unknown passkey or user`() {
        // When
        val byId = credentials.findByCredentialId(Bytes.random())
        val byUser = credentials.findByUserId(Bytes.random())

        // Then
        byId.shouldBeNull()
        byUser.shouldBeEmpty()
    }

    @Test
    fun `finds only the passkeys of the user`() {
        // Given
        val first = aPasskey()
        val second = aPasskey()
        credentials.save(first)
        credentials.save(second)
        credentials.save(aPasskey(user = Bytes.random()))

        // When
        val found = credentials.findByUserId(user)

        // Then
        found.map { it.credentialId } shouldContainExactlyInAnyOrder listOf(first.credentialId, second.credentialId)
    }

    @Test
    fun `updates a passkey saved again, as after a sign-in`() {
        // Given
        val passkey = aPasskey()
        credentials.save(passkey)
        val used = ImmutableCredentialRecord.fromCredentialRecord(passkey)
            .signatureCount(passkey.signatureCount + 1)
            .lastUsed(passkey.lastUsed.plusSeconds(60))
            .build()

        // When
        credentials.save(used)

        // Then
        credentials.findByUserId(user).map { it.fields() } shouldBe listOf(used.fields())
    }

    @Test
    fun `deletes a passkey`() {
        // Given
        val passkey = aPasskey()
        credentials.save(passkey)

        // When
        credentials.delete(passkey.credentialId)

        // Then
        credentials.findByCredentialId(passkey.credentialId).shouldBeNull()
    }

    protected fun aPasskey(user: Bytes = this.user): CredentialRecord = ImmutableCredentialRecord.builder()
        .credentialId(Bytes.random())
        .userEntityUserId(user)
        .publicKey(ImmutablePublicKeyCose(Bytes.random().bytes))
        .signatureCount(3)
        .uvInitialized(true)
        .backupEligible(true)
        .transports(setOf(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID))
        .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
        .backupState(true)
        .attestationObject(Bytes.random())
        .attestationClientDataJSON(Bytes.random())
        .created(Instant.parse("2026-11-20T10:00:00Z"))
        .lastUsed(Instant.parse("2026-11-21T10:00:00Z"))
        .label("Ada's phone")
        .build()

    // CredentialRecord has no equals
    private fun CredentialRecord.fields() = listOf(
        credentialId,
        userEntityUserId,
        Bytes(publicKey.bytes),
        signatureCount,
        isUvInitialized,
        isBackupEligible,
        transports,
        credentialType,
        isBackupState,
        attestationObject,
        attestationClientDataJSON,
        created,
        lastUsed,
        label,
    )
}
