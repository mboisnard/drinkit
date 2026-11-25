package com.drinkit.security.infra.passkey

import com.drinkit.postgresql.toStoredTime
import com.drinkit.security.generated.jooq.tables.records.UserCredentialsRecord
import com.drinkit.security.generated.jooq.tables.references.USER_CREDENTIALS
import com.drinkit.security.generated.jooq.tables.references.USER_ENTITIES
import com.drinkit.security.infra.events.SecurityEventLogger
import org.jooq.DSLContext
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.transaction.annotation.Transactional

/**
 * Spring Security's JdbcUserCredentialRepository in jOOQ, for its table in the security schema: same columns, same
 * base64url ids, transports joined by commas. Logs the passkeys added and removed, which Spring does not.
 *
 * W3C WebAuthn Level 3, 7.1 step 27 (credential record) and 6.1.1 (signature counter). Spring Security, Passkeys,
 * Persistence. OWASP: ASVS 5.0 V16.3.1, Logging Vocabulary (authn_token_created, authn_token_revoked).
 */
@Transactional
internal class JooqUserCredentials(private val dsl: DSLContext, private val events: SecurityEventLogger) :
    UserCredentialRepository {

    override fun findByCredentialId(credentialId: Bytes): CredentialRecord? = dsl.selectFrom(USER_CREDENTIALS)
        .where(USER_CREDENTIALS.CREDENTIAL_ID.eq(credentialId.toBase64UrlString()))
        .fetchOne()
        ?.toCredentialRecord()

    override fun findByUserId(userId: Bytes): List<CredentialRecord> = dsl.selectFrom(USER_CREDENTIALS)
        .where(USER_CREDENTIALS.USER_ENTITY_USER_ID.eq(userId.toBase64UrlString()))
        .orderBy(USER_CREDENTIALS.CREATED)
        .fetch { it.toCredentialRecord() }

    // Spring saves a credential again at each sign-in, with its new counter and last use: only an insert creates one
    override fun save(record: CredentialRecord) {
        val row = record.toRow()

        if (dsl.executeUpdate(row) == 0) {
            dsl.executeInsert(row)
            accountOf(row.userEntityUserId)?.let(events::passkeyCreated)
        }
    }

    override fun delete(credentialId: Bytes) {
        dsl.deleteFrom(USER_CREDENTIALS)
            .where(USER_CREDENTIALS.CREDENTIAL_ID.eq(credentialId.toBase64UrlString()))
            .returning(USER_CREDENTIALS.USER_ENTITY_USER_ID)
            .fetchOne(USER_CREDENTIALS.USER_ENTITY_USER_ID)
            ?.let(::accountOf)
            ?.let(events::passkeyRevoked)
    }

    private fun accountOf(userEntityId: String) = dsl.select(USER_ENTITIES.NAME)
        .from(USER_ENTITIES)
        .where(USER_ENTITIES.ID.eq(userEntityId))
        .fetchOne(USER_ENTITIES.NAME)

    private fun CredentialRecord.toRow() = UserCredentialsRecord(
        credentialId = credentialId.toBase64UrlString(),
        userEntityUserId = userEntityUserId.toBase64UrlString(),
        publicKey = publicKey.bytes,
        signatureCount = signatureCount,
        uvInitialized = isUvInitialized,
        backupEligible = isBackupEligible,
        authenticatorTransports = transports.joinToString(",") { it.value },
        publicKeyCredentialType = credentialType?.value,
        backupState = isBackupState,
        attestationObject = attestationObject?.bytes,
        attestationClientDataJson = attestationClientDataJSON?.bytes,
        created = created.toStoredTime(),
        lastUsed = lastUsed.toStoredTime(),
        label = label,
    )

    private fun UserCredentialsRecord.toCredentialRecord(): CredentialRecord = ImmutableCredentialRecord.builder()
        .credentialId(Bytes.fromBase64(credentialId))
        .userEntityUserId(Bytes.fromBase64(userEntityUserId))
        .publicKey(ImmutablePublicKeyCose(publicKey))
        .signatureCount(signatureCount ?: 0)
        .uvInitialized(uvInitialized == true)
        .backupEligible(backupEligible)
        .transports(
            authenticatorTransports.orEmpty().split(",").filter { it.isNotEmpty() }
                .map(AuthenticatorTransport::valueOf)
                .toSet(),
        )
        .credentialType(publicKeyCredentialType?.let(PublicKeyCredentialType::valueOf))
        .backupState(backupState)
        .attestationObject(attestationObject?.let(::Bytes))
        .attestationClientDataJSON(attestationClientDataJson?.let(::Bytes))
        .created(requireNotNull(created) { "A passkey has its creation time" }.toInstant())
        .lastUsed(requireNotNull(lastUsed) { "A passkey has its last use" }.toInstant())
        .label(label)
        .build()
}
