package com.drinkit.event.sourcing.transaction

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.SavepointManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate

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
    fun `with no open transaction, each attempt runs in a new transaction`() {
        runner.withBean(SaveEvent::class.java, { SaveEvent(refusals = 2) })
            .run { context ->
                // When
                context.getBean(SaveEvent::class.java).invoke()

                // Then
                context.getBean(SpyTransactionManager::class.java).begunTransactions shouldBe 3
            }
    }

    @Test
    fun `inside an open transaction, each attempt runs in a savepoint of it`() {
        runner.withBean(SaveEvent::class.java, { SaveEvent(refusals = 2) })
            .run { context ->
                val transactionManager = context.getBean(SpyTransactionManager::class.java)

                // When
                TransactionTemplate(transactionManager).execute { context.getBean(SaveEvent::class.java).invoke() }

                // Then
                transactionManager.begunTransactions shouldBe 1
                transactionManager.savepoints shouldBe 3
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
        var savepoints = 0
            private set
        private var open = false

        init {
            // As the JDBC transaction manager of the application allows them
            isNestedTransactionAllowed = true
        }

        override fun doGetTransaction(): Any = SpyTransaction()

        override fun isExistingTransaction(transaction: Any) = open

        override fun doBegin(transaction: Any, definition: TransactionDefinition) {
            begunTransactions++
            open = true
        }

        override fun doCommit(status: DefaultTransactionStatus) = Unit

        override fun doRollback(status: DefaultTransactionStatus) = Unit

        override fun doCleanupAfterCompletion(transaction: Any) {
            open = false
        }

        inner class SpyTransaction : SavepointManager {
            override fun createSavepoint(): Any = Any().also { savepoints++ }

            override fun rollbackToSavepoint(savepoint: Any) = Unit

            override fun releaseSavepoint(savepoint: Any) = Unit
        }
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
