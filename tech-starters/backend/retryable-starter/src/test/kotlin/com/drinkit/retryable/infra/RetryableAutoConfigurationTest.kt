package com.drinkit.retryable.infra

import com.drinkit.retryable.SpyRetryListener
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.resilience.annotation.Retryable

@ExtendWith(OutputCaptureExtension::class)
internal class RetryableAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RetryableAutoConfiguration::class.java))

    @Test
    fun `a failed call of a retryable method runs again`() {
        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 1) })
            .run { context ->
                // When
                val succeedingAttempt = context.getBean<FlakyCall>().call()

                // Then
                succeedingAttempt shouldBe 2
            }
    }

    @Test
    fun `each failed attempt is logged`(output: CapturedOutput) {
        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 2) })
            .run { context ->
                // When
                context.getBean<FlakyCall>().call()

                // Then
                FAILED_ATTEMPT_LOG.findAll(output.out).toList() shouldHaveSize 2
            }
    }

    @Test
    fun `a failed attempt is logged without the message of its failure`(output: CapturedOutput) {
        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 1) })
            .run { context ->
                // When
                context.getBean<FlakyCall>().call()

                // Then
                output.out shouldNotContain FAILURE_MESSAGE
            }
    }

    @Test
    fun `the end of the retries is logged`(output: CapturedOutput) {
        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 5) })
            .run { context ->
                // When
                shouldThrow<IllegalStateException> { context.getBean<FlakyCall>().call() }

                // Then
                STOPPED_RETRIES_LOG.findAll(output.out).toList() shouldHaveSize 1
                output.out shouldContain
                    "stopped after 3 attempts, the last one failed with java.lang.IllegalStateException"
            }
    }

    @Test
    fun `retries stopped by a failure the policy does not cover are logged with its class`(output: CapturedOutput) {
        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 0) })
            .run { context ->
                // When
                shouldThrow<IllegalArgumentException> { context.getBean<FlakyCall>().failThenRefuse() }

                // Then
                output.out shouldContain
                    "stopped after 2 attempts, the last one failed with java.lang.IllegalArgumentException"
            }
    }

    @Test
    fun `a first failure the retry policy does not cover is not logged as stopped retries`(output: CapturedOutput) {
        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 0) })
            .run { context ->
                // When
                shouldThrow<IllegalArgumentException> { context.getBean<FlakyCall>().refuse() }

                // Then
                output.out shouldNotContain STOPPED_RETRIES_LOG
            }
    }

    @Test
    fun `the application's own retry listener replaces the starter's`(output: CapturedOutput) {
        val applicationListener = SpyRetryListener()

        runner.withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 5) })
            .withBean(SpyRetryListener::class.java, { applicationListener })
            .run { context ->
                // When
                shouldThrow<IllegalStateException> { context.getBean<FlakyCall>().call() }

                // Then
                applicationListener.countFailedAttempts() shouldBe 3
                output.out shouldNotContain FAILED_ATTEMPT_LOG
            }
    }

    @Test
    fun `a failed call fails at once when the starter is switched off`() {
        runner.withPropertyValues("starters.retryable.enabled=false")
            .withBean(FlakyCall::class.java, { FlakyCall(failuresBeforeSuccess = 1) })
            .run { context ->
                // When
                val flakyCall = context.getBean<FlakyCall>()
                shouldThrow<IllegalStateException> { flakyCall.call() }

                // Then
                flakyCall.attempts shouldBe 1
            }
    }

    // Open by hand: the Kotlin Spring plugin opens @Transactional classes, not @Retryable ones
    internal open class FlakyCall(private val failuresBeforeSuccess: Int) {
        var attempts = 0
            private set

        @Retryable(maxRetries = 2, delay = 0)
        open fun call(): Int {
            attempts++
            check(attempts > failuresBeforeSuccess) { "$FAILURE_MESSAGE $attempts" }
            return attempts
        }

        @Retryable(includes = [IllegalStateException::class], maxRetries = 2, delay = 0)
        open fun refuse(): Nothing = throw IllegalArgumentException(FAILURE_MESSAGE)

        @Retryable(includes = [IllegalStateException::class], maxRetries = 2, delay = 0)
        open fun failThenRefuse(): Nothing {
            attempts++
            check(attempts > 1) { FAILURE_MESSAGE }
            throw IllegalArgumentException(FAILURE_MESSAGE)
        }
    }

    companion object {
        private val FAILED_ATTEMPT_LOG = Regex("Attempt of \\S+FlakyCall\\.call failed")
        private val STOPPED_RETRIES_LOG = Regex("Retries of \\S+FlakyCall\\.\\w+ stopped")
        private const val FAILURE_MESSAGE = "Personal data of attempt"
    }
}
