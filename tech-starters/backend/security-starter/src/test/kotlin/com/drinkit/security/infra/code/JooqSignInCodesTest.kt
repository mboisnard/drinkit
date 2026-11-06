package com.drinkit.security.infra.code

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.generated.jooq.DrinkitSession
import com.drinkit.security.generated.jooq.tables.references.SIGN_IN_CODE
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionOperations
import java.time.Duration

@JooqIntegrationTest(schemas = [DrinkitSession::class])
internal class JooqSignInCodesTest : SignInCodesTestContract() {

    override lateinit var codes: SignInCodes
    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
        codes = JooqSignInCodes(dsl, TransactionOperations.withoutTransaction())
    }

    @Test
    fun `deletes the codes whose expiry and reset of tries have passed`() {
        // Given
        val earlier = now.minus(Duration.ofHours(2))
        codes.replace(
            SignInCode("expired@drinkit.test", "hash", earlier.plusSeconds(600), 5, earlier.plusSeconds(3600)),
            earlier,
        )

        // When
        codes.deleteExpiredAt(now)

        // Then
        dsl.fetchCount(SIGN_IN_CODE) shouldBe 0
    }
}
