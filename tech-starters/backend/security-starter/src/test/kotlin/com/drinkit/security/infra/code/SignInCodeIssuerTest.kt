package com.drinkit.security.infra.code

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.generated.jooq.DrinkitSession
import com.drinkit.security.generated.jooq.tables.references.SIGN_IN_CODE
import com.drinkit.security.infra.SecurityStarterProperties
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionOperations
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

private const val LOGIN = "ada@drinkit.test"
private val NOW = Instant.parse("2026-11-06T10:00:00Z")

@JooqIntegrationTest(schemas = [DrinkitSession::class])
internal class SignInCodeIssuerTest {

    private val sender = SpySignInCodeSender()
    private lateinit var dsl: DSLContext
    private lateinit var issuer: SignInCodeIssuer

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
        val codes = JooqSignInCodes(dsl, TransactionOperations.withoutTransaction())
        issuer = SignInCodeIssuer(codes, sender, Clock.fixed(NOW, ZoneOffset.UTC), SecurityStarterProperties.Code())
    }

    @Test
    fun `keeps no value equal to the code it sends`() {
        // When
        issuer.issue(LOGIN, Locale.FRENCH)

        // Then
        val stored = dsl.fetch(SIGN_IN_CODE).flatMap { row -> row.intoList().map { it.toString() } }
        stored shouldNotContain sender.lastCodeOf(LOGIN)
    }

    @Test
    fun `gives a code 10 minutes to live`() {
        // When
        issuer.issue(LOGIN, Locale.FRENCH)

        // Then
        dsl.fetchSingle(SIGN_IN_CODE).expiresAt.toInstant() shouldBe NOW + Duration.ofMinutes(10)
    }
}
