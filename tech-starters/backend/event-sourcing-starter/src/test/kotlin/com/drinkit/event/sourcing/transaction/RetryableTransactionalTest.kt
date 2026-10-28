package com.drinkit.event.sourcing.transaction

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus

internal class RetryableTransactionalTest {

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(TransactionsConfiguration::class.java)

    @Test
    fun `a write refused on a taken sequence id runs again`() {
        runner.withBean(SaveEvent::class.java, { SaveEvent(refusals = 1) })
            .run { context ->
                // When
                val succeedingAttempt = context.getBean(SaveEvent::class.java).invoke()

                // Then
                succeedingAttempt shouldBe 2
            }
    }

    @Test
    fun `a write refused at every attempt fails after three attempts`() {
        runner.withBean(SaveEvent::class.java, { SaveEvent(refusals = 5) })
            .run { context ->
                // When
                val saveEvent = context.getBean(SaveEvent::class.java)
                shouldThrow<DuplicateSequenceException> { saveEvent.invoke() }

                // Then
                saveEvent.attempts shouldBe 3
            }
    }

    @Test
    fun `a failure other than a refused sequence id is not retried`() {
        runner.withBean(SaveEvent::class.java, { SaveEvent(refusals = 0, otherFailure = true) })
            .run { context ->
                // When
                val saveEvent = context.getBean(SaveEvent::class.java)
                shouldThrow<IllegalStateException> { saveEvent.invoke() }

                // Then
                saveEvent.attempts shouldBe 1
            }
    }

    @Test
    fun `each attempt runs in a transaction of its own`() {
        runner.withBean(SaveEvent::class.java, { SaveEvent(refusals = 2) })
            .run { context ->
                // When
                context.getBean(SaveEvent::class.java).invoke()

                // Then
                context.getBean(SpyTransactionManager::class.java).begunTransactions shouldBe 3
            }
    }

    @RetryableTransactional
    internal class SaveEvent(private val refusals: Int, private val otherFailure: Boolean = false) {
        var attempts = 0

        fun invoke(): Int {
            attempts++
            check(!otherFailure) { "Not a refused sequence id" }
            if (attempts <= refusals) {
                throw DuplicateSequenceException("Sequence id taken at attempt $attempts", cause = null)
            }
            return attempts
        }
    }

    internal class SpyTransactionManager : AbstractPlatformTransactionManager() {
        var begunTransactions = 0
            private set

        override fun doGetTransaction(): Any = Any()

        override fun doBegin(transaction: Any, definition: TransactionDefinition) {
            begunTransactions++
        }

        override fun doCommit(status: DefaultTransactionStatus) = Unit

        override fun doRollback(status: DefaultTransactionStatus) = Unit
    }

    // The retry comes from retryable-starter, found on the classpath as an application finds it
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableTransactionManagement
    class TransactionsConfiguration {

        @Bean
        fun transactionManager() = SpyTransactionManager()
    }
}
