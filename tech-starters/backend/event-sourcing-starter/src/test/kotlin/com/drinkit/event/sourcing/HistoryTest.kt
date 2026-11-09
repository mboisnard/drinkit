package com.drinkit.event.sourcing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
    fun `holds its initial event alone when nothing followed it`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL",
        )

        // When
        val history = CarHistory(initEvent)

        // Then
        history.initEvent shouldBe initEvent
        history.remainingEvents shouldBe emptyList()
    }

    @Test
    fun `keeps the initial event apart from the following ones`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL",
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
            owner = "James Bond",
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(20),
            date = OffsetDateTime.now(),
        )

        // When
        val history = CarHistory(
            initEvent = initEvent,
            remainingEvents = listOf(event1, event2),
        )

        // Then
        history.initEvent shouldBe initEvent
        history.remainingEvents shouldBe listOf(event1, event2)
    }

    @Test
    fun `starts from its initial event and keeps the following ones in order`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL",
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
            owner = "James Bond",
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(20),
            date = OffsetDateTime.now(),
        )
        val events = listOf(initEvent, event1, event2)

        // When
        val history = CarHistory.from<CarEvent, CarCreated>(events)

        // Then
        history.initEvent shouldBe initEvent
        history.remainingEvents shouldBe listOf(event1, event2)
    }

    @Test
    fun `refuses no event`() {
        // When & Then
        shouldThrow<IllegalArgumentException> {
            CarHistory.from<CarEvent, CarCreated>(emptyList())
        }
    }

    @Test
    fun `refuses events that do not start with the initial event`() {
        // Given
        val carId = UUID.randomUUID()
        val event1 = CarPurchased(
            id = carId,
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            owner = "James Bond",
        )
        val event2 = MaintenanceCarriedOut(
            id = carId,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
        )
        val events = listOf(event1, event2)

        // When & Then
        shouldThrow<IllegalArgumentException> {
            CarHistory.from<CarEvent, CarCreated>(events)
        }
    }

    @Test
    fun `refuses events whose sequence ids go down`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL",
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(20),
            date = OffsetDateTime.now(),
            owner = "James Bond",
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
        )
        val events = listOf(initEvent, event1, event2)

        // When & Then
        shouldThrow<IllegalArgumentException> {
            CarHistory.from<CarEvent, CarCreated>(events)
        }
    }

    @Test
    fun `refuses two events of the same sequence id`() {
        // Given
        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Mercedes-Benz 300 SL",
        )
        val event1 = CarPurchased(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
            owner = "James Bond",
        )
        val event2 = MaintenanceCarriedOut(
            id = initEvent.id,
            sequenceId = SequenceId(10),
            date = OffsetDateTime.now(),
        )
        val events = listOf(initEvent, event1, event2)

        // When & Then
        shouldThrow<IllegalArgumentException> {
            CarHistory.from<CarEvent, CarCreated>(events)
        }
    }
}
