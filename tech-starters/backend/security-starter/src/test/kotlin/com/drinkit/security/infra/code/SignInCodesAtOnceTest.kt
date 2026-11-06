package com.drinkit.security.infra.code

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.PostgresSessionsTest
import com.drinkit.security.generated.jooq.DrinkitSession
import com.drinkit.security.infra.hashOf
import com.drinkit.test.ControlledClock
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

private const val LOGIN = "ada@drinkit.test"
private const val CODE = "042137"
private const val REQUESTS = 8

@JooqIntegrationTest(schemas = [DrinkitSession::class])
@PostgresSessionsTest
internal class SignInCodesAtOnceTest(private val codes: SignInCodes, private val clock: ControlledClock) {

    private fun givenALiveCode() {
        val now = clock.instant()
        codes.replace(SignInCode(LOGIN, hashOf(CODE), now + Duration.ofMinutes(10), 3, now + Duration.ofHours(1)), now)
    }

    private fun tryAtOnce(times: Int, code: String): List<Boolean> {
        val start = CountDownLatch(1)
        return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val tries = List(times) {
                executor.submit<Boolean> {
                    start.await()
                    codes.tryCode(LOGIN, hashOf(code), clock.instant())
                }
            }
            start.countDown()
            tries.map { it.get() }
        }
    }

    @Test
    fun `accepts the right code once when it is tried many times at once`() {
        // Given
        givenALiveCode()

        // When
        val accepted = tryAtOnce(REQUESTS, CODE)

        // Then
        accepted.count { it } shouldBe 1
    }

    @Test
    fun `counts every wrong try sent at once`() {
        // Given
        givenALiveCode()
        tryAtOnce(3, "000000")

        // When
        val accepted = codes.tryCode(LOGIN, hashOf(CODE), clock.instant())

        // Then
        accepted shouldBe false
    }
}
