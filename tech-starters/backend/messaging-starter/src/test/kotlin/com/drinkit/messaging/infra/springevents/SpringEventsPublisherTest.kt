package com.drinkit.messaging.infra.springevents

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventPublisherTestContract
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import kotlin.reflect.KClass

internal class SpringEventsPublisherTest : PlatformEventPublisherTestContract() {

    private val deliveries = mutableListOf<Delivery>()
    private val springEvents get() = deliveries.map { it.event }
    override val publisher = SpringEventsPublisher { deliveries += it as Delivery }
    private val transaction = TransactionTemplate(NoOpTransactionManager())

    override fun countPublished(type: KClass<out PlatformEvent<*>>): Int =
        deliveries.count { type.isInstance(it.event) }

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

    @Test
    fun `an event published outside any request or event carries a key of its own`() {
        // When
        publisher.publish(SomethingHappened(1))
        publisher.publish(SomethingHappened(1))

        // Then
        deliveries.map { it.idempotencyKey }.shouldNotContainDuplicates()
    }

    @Test
    fun `a replayed request publishes its events under the same keys`() {
        // Given
        CurrentIdempotencyKey.with("account-creation-key") { publisher.publish(SomethingHappened(1)) }

        // When
        CurrentIdempotencyKey.with("account-creation-key") { publisher.publish(SomethingHappened(1)) }

        // Then
        val (first, retry) = deliveries.map { it.idempotencyKey }
        retry shouldBe first
        first shouldNotBe "account-creation-key"
    }

    @Test
    fun `events published by one request each get their own key`() {
        // When
        CurrentIdempotencyKey.with("cellars-creation-key") {
            publisher.publish(SomethingHappened(1))
            publisher.publish(SomethingHappened(2))
        }

        // Then
        deliveries.map { it.idempotencyKey }.shouldNotContainDuplicates()
    }
}
