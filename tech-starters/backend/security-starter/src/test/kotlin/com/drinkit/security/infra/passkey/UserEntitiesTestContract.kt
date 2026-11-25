package com.drinkit.security.infra.passkey

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository

/** The behavior of Spring Security's own repository tests, for the jOOQ one and the in-memory one of Spring. */
internal abstract class UserEntitiesTestContract {

    abstract val userEntities: PublicKeyCredentialUserEntityRepository

    private val userEntity = ImmutablePublicKeyCredentialUserEntity.builder()
        .id(Bytes.random())
        .name("user-1")
        .displayName("user-1")
        .build()

    @Test
    fun `finds a saved user entity by its id and by its name`() {
        // When
        userEntities.save(userEntity)

        // Then
        userEntities.findById(userEntity.id) shouldBe userEntity
        userEntities.findByUsername("user-1") shouldBe userEntity
    }

    @Test
    fun `finds nothing for an unknown id or name`() {
        // When
        val byId = userEntities.findById(Bytes.random())
        val byName = userEntities.findByUsername("nobody")

        // Then
        byId.shouldBeNull()
        byName.shouldBeNull()
    }

    @Test
    fun `updates a user entity saved again`() {
        // Given
        userEntities.save(userEntity)
        val renamed = ImmutablePublicKeyCredentialUserEntity.builder()
            .id(userEntity.id)
            .name("user-1")
            .displayName("Ada")
            .build()

        // When
        userEntities.save(renamed)

        // Then
        userEntities.findById(userEntity.id) shouldBe renamed
    }

    @Test
    fun `deletes a user entity`() {
        // Given
        userEntities.save(userEntity)

        // When
        userEntities.delete(userEntity.id)

        // Then
        userEntities.findById(userEntity.id).shouldBeNull()
        userEntities.findByUsername("user-1").shouldBeNull()
    }
}
