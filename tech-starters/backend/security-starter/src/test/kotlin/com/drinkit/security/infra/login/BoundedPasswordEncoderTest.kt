package com.drinkit.security.infra.login

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

internal class BoundedPasswordEncoderTest {

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `makes a hash wait while every permit is taken`() {
        // Given
        val permits = Semaphore(1)
        val hashes = CountingEncoder()
        val encoder = BoundedPasswordEncoder(hashes, permits)
        permits.acquire()

        // When
        val login = Thread.ofVirtual().start { encoder.matches("password", "hash") }
        while (!permits.hasQueuedThreads()) Thread.onSpinWait()

        // Then
        hashes.count shouldBe 0
        permits.release()
        login.join()
        hashes.count shouldBe 1
    }

    @Test
    fun `gives its permit back after a hash that fails`() {
        // Given
        val permits = Semaphore(1)
        val encoder = BoundedPasswordEncoder(FailingEncoder, permits)

        // When
        shouldThrow<IllegalStateException> { encoder.encode("password") }

        // Then
        permits.availablePermits() shouldBe 1
    }

    private class CountingEncoder : PasswordEncoder {
        private val calls = AtomicInteger()
        val count get() = calls.get()

        override fun encode(rawPassword: CharSequence?): String = "hash".also { calls.incrementAndGet() }

        override fun matches(rawPassword: CharSequence?, encodedPassword: String?) = true.also {
            calls.incrementAndGet()
        }
    }

    private object FailingEncoder : PasswordEncoder {
        override fun encode(rawPassword: CharSequence?): String = error("Out of memory")

        override fun matches(rawPassword: CharSequence?, encodedPassword: String?) = error("Out of memory")
    }
}
