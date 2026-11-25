package com.drinkit.security.infra.passkey

import com.drinkit.security.generated.jooq.tables.records.UserEntitiesRecord
import com.drinkit.security.generated.jooq.tables.references.USER_ENTITIES
import org.jooq.DSLContext
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.transaction.annotation.Transactional

/**
 * Spring Security's JdbcPublicKeyCredentialUserEntityRepository in jOOQ, for its table in the security schema: same
 * columns, same base64url ids. The name of a user entity is the id of its account.
 *
 * W3C WebAuthn Level 3, 5.4.3 (user entity) and 14.6.1 (user handle). Spring Security, Passkeys, Persistence.
 */
@Transactional
internal class JooqUserEntities(private val dsl: DSLContext) : PublicKeyCredentialUserEntityRepository {

    override fun findById(id: Bytes): PublicKeyCredentialUserEntity? = dsl.selectFrom(USER_ENTITIES)
        .where(USER_ENTITIES.ID.eq(id.toBase64UrlString()))
        .fetchOne()
        ?.toUserEntity()

    override fun findByUsername(username: String): PublicKeyCredentialUserEntity? = dsl.selectFrom(USER_ENTITIES)
        .where(USER_ENTITIES.NAME.eq(username))
        .fetchOne()
        ?.toUserEntity()

    override fun save(userEntity: PublicKeyCredentialUserEntity) {
        val row = UserEntitiesRecord(userEntity.id.toBase64UrlString(), userEntity.name, userEntity.displayName)

        if (dsl.executeUpdate(row) == 0) dsl.executeInsert(row)
    }

    override fun delete(id: Bytes) {
        dsl.deleteFrom(USER_ENTITIES).where(USER_ENTITIES.ID.eq(id.toBase64UrlString())).execute()
    }

    private fun UserEntitiesRecord.toUserEntity(): PublicKeyCredentialUserEntity =
        ImmutablePublicKeyCredentialUserEntity.builder()
            .id(Bytes.fromBase64(id))
            .name(name)
            .displayName(displayName)
            .build()
}
