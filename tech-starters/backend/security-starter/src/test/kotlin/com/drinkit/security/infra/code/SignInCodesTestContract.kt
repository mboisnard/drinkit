package com.drinkit.security.infra.code

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val LOGIN = "ada@drinkit.test"
private const val HASH = "hash-of-the-code"
private const val TRIES = 3
private val WINDOW = Duration.ofHours(1)

/** The behavior every [SignInCodes] keeps, in memory as in PostgreSQL. */
internal abstract class SignInCodesTestContract {

    protected abstract val codes: SignInCodes
    protected val now: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

    private fun aCode(
        login: String = LOGIN,
        hash: String = HASH,
        expiresAt: Instant = now.plusSeconds(600),
        at: Instant = now,
    ) = SignInCode(login, hash, expiresAt, TRIES, at + WINDOW)

    private fun SignInCodes.replace(code: SignInCode) = replace(code, now)

    @Test
    fun `accepts the live code of a login once`() {
        // Given
        codes.replace(aCode())

        // When
        val first = codes.tryCode(LOGIN, HASH, now)
        val second = codes.tryCode(LOGIN, HASH, now)

        // Then
        first shouldBe true
        second shouldBe false
    }

    @Test
    fun `refuses a code at its expiry`() {
        // Given
        codes.replace(aCode(expiresAt = now))

        // When
        val accepted = codes.tryCode(LOGIN, HASH, now)

        // Then
        accepted shouldBe false
    }

    @Test
    fun `refuses the right code once its tries are spent on wrong ones`() {
        // Given
        codes.replace(aCode())
        repeat(TRIES) { codes.tryCode(LOGIN, "a wrong hash", now) }

        // When
        val accepted = codes.tryCode(LOGIN, HASH, now)

        // Then
        accepted shouldBe false
    }

    @Test
    fun `accepts the right code after fewer wrong ones than its tries`() {
        // Given
        codes.replace(aCode())
        repeat(TRIES - 1) { codes.tryCode(LOGIN, "a wrong hash", now) }

        // When
        val accepted = codes.tryCode(LOGIN, HASH, now)

        // Then
        accepted shouldBe true
    }

    @Test
    fun `keeps only the latest code of a login`() {
        // Given
        codes.replace(aCode(hash = "earlier"))
        codes.replace(aCode(hash = "latest"))

        // When
        val earlier = codes.tryCode(LOGIN, "earlier", now)
        val latest = codes.tryCode(LOGIN, "latest", now)

        // Then
        earlier shouldBe false
        latest shouldBe true
    }

    @Test
    fun `keeps the tries spent on an earlier code for the next code of the same login`() {
        // Given
        codes.replace(aCode(hash = "earlier"))
        repeat(TRIES) { codes.tryCode(LOGIN, "a wrong hash", now) }
        codes.replace(aCode(hash = "latest"))

        // When
        val accepted = codes.tryCode(LOGIN, "latest", now)

        // Then
        accepted shouldBe false
    }

    @Test
    fun `gives the tries back once their window is over`() {
        // Given
        codes.replace(aCode(hash = "earlier"))
        repeat(TRIES) { codes.tryCode(LOGIN, "a wrong hash", now) }
        val later = now + WINDOW
        codes.replace(aCode(hash = "latest", expiresAt = later.plusSeconds(600), at = later), later)

        // When
        val accepted = codes.tryCode(LOGIN, "latest", later)

        // Then
        accepted shouldBe true
    }

    @Test
    fun `refuses the code of another login`() {
        // Given
        codes.replace(aCode())

        // When
        val accepted = codes.tryCode("grace@drinkit.test", HASH, now)

        // Then
        accepted shouldBe false
    }

    @Test
    fun `keeps the tries spent on an expired code until their window is over`() {
        // Given
        codes.replace(aCode(hash = "earlier"))
        repeat(TRIES) { codes.tryCode(LOGIN, "a wrong hash", now) }
        val afterExpiry = now.plusSeconds(601)
        codes.deleteExpiredAt(afterExpiry)
        codes.replace(aCode(hash = "latest", expiresAt = afterExpiry.plusSeconds(600), at = afterExpiry), afterExpiry)

        // When
        val accepted = codes.tryCode(LOGIN, "latest", afterExpiry)

        // Then
        accepted shouldBe false
    }

    @Test
    fun `keeps the live codes when it forgets the expired ones`() {
        // Given
        val earlier = now - WINDOW
        codes.replace(
            aCode(login = "expired@drinkit.test", expiresAt = earlier.plusSeconds(600), at = earlier),
            earlier,
        )
        codes.replace(aCode())

        // When
        codes.deleteExpiredAt(now)

        // Then
        codes.tryCode(LOGIN, HASH, now) shouldBe true
    }
}
