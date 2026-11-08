package com.drinkit.idempotency.infra

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.IdempotentResult
import com.drinkit.test.ControlledClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.time.Duration
import java.time.Instant

internal abstract class IdempotencyTestContract {

    protected val clock = ControlledClock().fix(Instant.parse("2026-11-08T10:00:00Z"))

    protected val properties = IdempotencyProperties(retention = Duration.ofHours(24), lease = Duration.ofMinutes(5))

    protected val idempotency: Idempotency by lazy { fetchIdempotency() }

    abstract fun fetchIdempotency(): Idempotency

    private var runs = 0

    private fun sendWelcomeEmail(): String {
        runs++
        return "sent"
    }

    @Test
    fun `hands back what the first run recorded instead of running again an action done under its key`() {
        // Given
        idempotency.once("welcome-email", "key-1", record = { it.toByteArray() }) { sendWelcomeEmail() }

        // When
        val replay = idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }

        // Then
        runs shouldBe 1
        replay.shouldBeInstanceOf<IdempotentResult.AlreadyDone>().recorded?.decodeToString() shouldBe "sent"
    }

    @Test
    fun `tells a replay arriving while the first run goes on to come back once its lease ends`() {
        // When
        val replay = idempotency.once("welcome-email", "key-1") {
            clock.add(Duration.ofMinutes(1))
            idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }
        }

        // Then
        runs shouldBe 0
        replay shouldBe IdempotentResult.Executed(IdempotentResult.InFlight(Duration.ofMinutes(4)))
    }

    @Test
    fun `refuses a key reused for another payload`() {
        // Given
        idempotency.once("cellar-creation", "key-1", fingerprint = "Cave du Rhône") { sendWelcomeEmail() }

        // When
        val other = idempotency.once("cellar-creation", "key-1", fingerprint = "Cave de Loire") { sendWelcomeEmail() }

        // Then
        other shouldBe IdempotentResult.Mismatch
    }

    @Test
    fun `runs again an action whose first run failed`() {
        // Given
        shouldThrow<IllegalStateException> {
            idempotency.once("welcome-email", "key-1") { error("Mail server down") }
        }

        // When
        val retry = idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }

        // Then
        retry shouldBe IdempotentResult.Executed("sent")
    }

    @Test
    fun `keeps only the outcome of the run that took over a key whose first run outlived its lease`() {
        // Given
        var takeOver: IdempotentResult<String>? = null

        // When
        val firstRun = runCatching {
            idempotency.once("welcome-email", "key-1", record = { it.toByteArray() }) {
                clock.add(properties.lease + Duration.ofSeconds(1))
                takeOver = idempotency.once("welcome-email", "key-1", record = { it.toByteArray() }) { "second" }
                "first"
            }
        }

        // Then
        takeOver shouldBe IdempotentResult.Executed("second")
        firstRun.isFailure shouldBe true
        val replay = idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }
        replay.shouldBeInstanceOf<IdempotentResult.AlreadyDone>().recorded?.decodeToString() shouldBe "second"
    }

    @Test
    fun `runs once per scope an action sent under one key`() {
        // Given
        idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }

        // When
        val other = idempotency.once("search-indexing", "key-1") { sendWelcomeEmail() }

        // Then
        other shouldBe IdempotentResult.Executed("sent")
    }

    @Test
    fun `forgets a key once its retention ends`() {
        // Given
        idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }
        clock.add(properties.retention + Duration.ofSeconds(1))

        // When
        val later = idempotency.once("welcome-email", "key-1") { sendWelcomeEmail() }

        // Then
        later shouldBe IdempotentResult.Executed("sent")
    }

    @Test
    fun `makes its key the current one while the action runs`() {
        // When
        val seen = idempotency.once(
            "welcome-email",
            "key-1",
        ) { CurrentIdempotencyKey.get() to MDC.get("idempotency.key") }

        // Then
        seen.shouldBeInstanceOf<IdempotentResult.Executed<Pair<String?, String?>>>().value.run {
            first shouldNotBe null
            second shouldBe first
        }
        CurrentIdempotencyKey.get() shouldBe null
    }
}
