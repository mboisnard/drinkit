package com.drinkit.security.infra.passkey

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.generated.jooq.DrinkitSecurity
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository

@JooqIntegrationTest(schemas = [DrinkitSecurity::class])
internal class JooqUserEntitiesTest : UserEntitiesTestContract() {

    override lateinit var userEntities: PublicKeyCredentialUserEntityRepository

    @BeforeEach
    fun setup(dsl: DSLContext) {
        userEntities = JooqUserEntities(dsl)
    }
}
