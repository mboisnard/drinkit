package com.drinkit.rate.limit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import java.time.Duration

internal class RateLimitDecisionTest {

    @Test
    fun `a refused decision throws a 429 with its retry delay rounded up to the second`() {
        // Given
        val refused = RateLimitDecision.Refused("signin", Duration.ofMillis(7_001))

        // When
        val exception = shouldThrow<RateLimitExceededException> { refused.orThrow() }

        // Then
        exception.statusCode shouldBe TOO_MANY_REQUESTS
        exception.headers.getFirst(RETRY_AFTER) shouldBe "8"
    }
}
