package com.drinkit.idempotency.infra

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.IdempotentResult
import com.drinkit.idempotency.InMemoryIdempotencyKeys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

internal class InMemoryIdempotencyTest : IdempotencyTestContract() {

    override fun fetchIdempotency(): Idempotency = KeyedIdempotency(InMemoryIdempotencyKeys(), clock, properties)

    @Test
    fun `runs again an action whose outcome the database failed to keep`() {
        // Given
        val keys = InMemoryIdempotencyKeys()
        var databaseDown = true
        val idempotency = KeyedIdempotency(
            object : IdempotencyKeys by keys {
                override fun complete(key: HeldKey, outcome: ByteArray?, at: Instant) {
                    check(!databaseDown) { "Database down" }
                    keys.complete(key, outcome, at)
                }
            },
            clock,
            properties,
        )
        shouldThrow<IllegalStateException> { idempotency.once("welcome-email", "key-1") { "sent" } }
        databaseDown = false

        // When
        val retry = idempotency.once("welcome-email", "key-1") { "sent again" }

        // Then
        retry shouldBe IdempotentResult.Executed("sent again")
    }
}
