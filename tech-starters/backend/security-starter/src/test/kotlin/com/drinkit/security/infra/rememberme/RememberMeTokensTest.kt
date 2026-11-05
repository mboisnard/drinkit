package com.drinkit.security.infra.rememberme

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.generated.jooq.DrinkitSession
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionOperations
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@JooqIntegrationTest(schemas = [DrinkitSession::class])
internal class RememberMeTokensTest {

    private lateinit var tokens: RememberMeTokens
    private val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

    @BeforeEach
    fun setup(dsl: DSLContext) {
        tokens = RememberMeTokens(dsl, TransactionOperations.withoutTransaction())
    }

    @Test
    fun `finds a series by its id`() {
        // Given
        val series = aSeries()

        // When
        tokens.create(series, "Firefox")

        // Then
        tokens.find(series.series) shouldBe series
    }

    @Test
    fun `keeps the instants of a series truncated to the microsecond, never later`() {
        // Given
        val withNanoseconds = now.plusNanos(600)
        val series = aSeries().copy(createdAt = withNanoseconds, rotatedAt = withNanoseconds)

        // When
        tokens.create(series, null)

        // Then
        tokens.find(series.series)?.createdAt shouldBe now
    }

    @Test
    fun `finds no unknown series`() {
        tokens.find("unknown").shouldBeNull()
    }

    @Test
    fun `rotates the token of a series, keeping the previous one`() {
        // Given
        val series = aSeries().also { tokens.create(it, null) }
        val later = now.plusSeconds(60)

        // When
        val rotated = tokens.rotate(series.series, series.tokenHash, "b2", later)

        // Then
        rotated shouldBe true
        tokens.find(series.series) shouldBe
            series.copy(tokenHash = "b2", previousTokenHash = series.tokenHash, rotatedAt = later)
    }

    @Test
    fun `rotates no token that another request already rotated`() {
        // Given
        val series = aSeries().also { tokens.create(it, null) }
        tokens.rotate(series.series, series.tokenHash, "b2", now)

        // When
        val rotated = tokens.rotate(series.series, series.tokenHash, "c3", now)

        // Then
        rotated shouldBe false
        tokens.find(series.series)?.tokenHash shouldBe "b2"
    }

    @Test
    fun `deletes one series`() {
        // Given
        val series = aSeries().also { tokens.create(it, null) }
        val other = aSeries(userId = series.userId).also { tokens.create(it, null) }

        // When
        tokens.delete(series.series)

        // Then
        tokens.find(series.series).shouldBeNull()
        tokens.find(other.series) shouldBe other
    }

    @Test
    fun `deletes every series of a user, and only them`() {
        // Given
        val laptop = aSeries().also { tokens.create(it, null) }
        val phone = aSeries(userId = laptop.userId).also { tokens.create(it, null) }
        val other = aSeries().also { tokens.create(it, null) }

        // When
        tokens.deleteAllOf(laptop.userId)

        // Then
        listOf(laptop, phone, other).map { tokens.find(it.series) } shouldBe listOf(null, null, other)
    }

    @Test
    fun `deletes the series created before an instant`() {
        // Given
        val old = aSeries(createdAt = now.minus(Duration.ofDays(31))).also { tokens.create(it, null) }
        val recent = aSeries(createdAt = now.minus(Duration.ofDays(29))).also { tokens.create(it, null) }

        // When
        tokens.deleteCreatedBefore(now.minus(Duration.ofDays(30)))

        // Then
        listOf(old, recent).map { tokens.find(it.series) } shouldBe listOf(null, recent)
    }

    private fun aSeries(userId: String = UUID.randomUUID().toString(), createdAt: Instant = now) = RememberMeSeries(
        series = UUID.randomUUID().toString(),
        userId = userId,
        tokenHash = "a1",
        previousTokenHash = null,
        rotatedAt = createdAt,
        createdAt = createdAt,
    )
}
