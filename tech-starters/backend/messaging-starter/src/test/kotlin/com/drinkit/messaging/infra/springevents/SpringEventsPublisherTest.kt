package com.drinkit.messaging.infra.springevents

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate

internal class SpringEventsPublisherTest {

    private val springEvents = mutableListOf<Any>()
    private val publisher = SpringEventsPublisher { springEvents += it }
    private val transaction = TransactionTemplate(NoOpTransactionManager())

    @Test
    fun `an event published in a transaction becomes a Spring event only once the transaction commits`() {
        transaction.executeWithoutResult {
            // When
            publisher.publish(SomethingHappened(1))

            // Then
            springEvents.shouldBeEmpty()
        }
        springEvents shouldContainExactly listOf(SomethingHappened(1))
    }

    @Test
    fun `an event published in a transaction that rolls back never becomes a Spring event`() {
        // When
        transaction.executeWithoutResult { status ->
            publisher.publish(SomethingHappened(1))
            status.setRollbackOnly()
        }

        // Then
        springEvents.shouldBeEmpty()
    }

    @Test
    fun `an event published outside a transaction becomes a Spring event at once`() {
        // When
        publisher.publish(SomethingHappened(1))

        // Then
        springEvents shouldContainExactly listOf(SomethingHappened(1))
    }

    @Test
    fun `two equal events published in one transaction both become Spring events`() {
        // When
        transaction.executeWithoutResult {
            publisher.publish(SomethingHappened(1))
            publisher.publish(SomethingHappened(1))
        }

        // Then
        springEvents shouldContainExactly listOf(SomethingHappened(1), SomethingHappened(1))
    }
}
