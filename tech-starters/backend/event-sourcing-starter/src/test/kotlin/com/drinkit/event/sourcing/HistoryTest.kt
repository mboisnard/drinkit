package com.drinkit.event.sourcing

import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

internal typealias CarHistory = History<CarEvent, CarCreated>

/**
 * This test class serves as documentation to help people understand how to use the History class.
 * It demonstrates how to create and work with History objects that contain domain events.
 */
internal class HistoryTest {

    @Test
    fun `should create History with init event only`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL"
        )

        // When
        val history = CarHistory(initEvent)

        // Then
        history.initEvent shouldBe initEvent
        history.remainingEvents shouldBe emptyList()
    }

    @Test
    fun `should create History with init event and remaining events`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL"
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
            owner = "James Bond"
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(20),
            date = OffsetDateTime.now()
        )

        // When
        val history = CarHistory(
            initEvent = initEvent,
            remainingEvents = listOf(event1, event2)
        )

        // Then
        history.initEvent shouldBe initEvent
        history.remainingEvents shouldBe listOf(event1, event2)
    }

    @Test
    fun `should create History with a list of events`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL"
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
            owner = "James Bond"
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(20),
            date = OffsetDateTime.now()
        )
        val events = listOf(initEvent, event1, event2)

        // When
        val history = CarHistory.from<CarEvent, CarCreated>(events)

        // Then
        history.initEvent shouldBe initEvent
        history.remainingEvents shouldBe listOf(event1, event2)
    }

    @Test
    fun `should throw exception when the list of events does not start with the init event`() {
        // Given
        val carId = UUID.randomUUID()
        val event1 = CarPurchased(
            id = carId,
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            owner = "James Bond"
        )
        val event2 = MaintenanceCarriedOut(
            id = carId,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now()
        )
        val events = listOf(event1, event2)

        // When & Then
        shouldThrow<IllegalArgumentException> {
            CarHistory.from<CarEvent, CarCreated>(events)
        }
    }

    @Test
    fun `should throw exception when events are not sequentially sorted`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL"
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(20),
            date = OffsetDateTime.now(),
            owner = "James Bond"
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now()
        )
        val events = listOf(initEvent, event1, event2)

        // When & Then
        shouldThrow<IllegalArgumentException> {
            CarHistory.from<CarEvent, CarCreated>(events)
        }
    }
}