package com.drinkit.event.sourcing

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This test class serves as documentation to help people understand how to use the EventsReducer.
 * It demonstrates a simple workflow with initialization and step events.
 */
internal class EventsReducerTest {

    @Test
    fun `folds each event in turn into the projection its initial event built`() {
        // Given
        val reducer = EventsReducer<CarProjection, CarEvent, CarCreated>(
            factory = CarProjection::applyInit,
        )
            .register<CarPurchased>(CarProjection::apply)
            .register<MaintenanceCarriedOut>(CarProjection::apply)

        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Aston Martin DB5",
        )
        val events = listOf(
            CarPurchased(
                id = initEvent.id,
                sequenceId = SequenceId(10),
                date = OffsetDateTime.now(),
                owner = "James Bond",
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(20),
                date = OffsetDateTime.now(),
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(30),
                date = OffsetDateTime.now(),
            ),
        )

        // When
        // Reduce the events to build the final state
        val result = reducer.reduce(initEvent, events)

        // Then
        result.id shouldBe initEvent.id
        result.name shouldBe initEvent.name
        result.owner shouldBe "James Bond"
        result.maintenanceCount shouldBe 2
        result.sequenceId shouldBe SequenceId(30)
    }

    @Test
    fun `hands an event type with no handler to the default handler`() {
        // Given
        val reducer = EventsReducer<CarProjection, CarEvent, CarCreated>(
            factory = CarProjection::applyInit,
            defaultHandler = { projection, event -> projection.copy(sequenceId = event.sequenceId) },
        )
            // Can also be written with .register(CarPurchased::class) { projection, event -> projection.apply(event) }
            // or .register<CarPurchased>(CarProjection::apply)
            .register(CarPurchased::class, CarProjection::apply)

        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Aston Martin DB5",
        )
        val events = listOf(
            CarPurchased(
                id = initEvent.id,
                sequenceId = SequenceId(10),
                date = OffsetDateTime.now(),
                owner = "James Bond",
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(20),
                date = OffsetDateTime.now(),
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(30),
                date = OffsetDateTime.now(),
            ),
        )

        // When

        // Reduce the events to build the final state, include MaintenanceCarriedOut which doesn't have a registered
        // handler
        val result = reducer.reduce(initEvent, events)

        // Then
        result.id shouldBe initEvent.id
        result.name shouldBe initEvent.name
        result.owner shouldBe "James Bond"
        result.maintenanceCount shouldBe 0 // Still 0 because MaintenanceCarriedOut was handled by the default handler
        result.sequenceId shouldBe SequenceId(30)
    }

    @Test
    fun `leaves the projection as it is for an event type with no handler`() {
        // Given
        val reducer = EventsReducer<CarProjection, CarEvent, CarCreated>(
            factory = CarProjection::applyInit,
        )
            .register<CarPurchased>(CarProjection::apply)

        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Aston Martin DB5",
        )
        val events = listOf(
            CarPurchased(
                id = initEvent.id,
                sequenceId = SequenceId(10),
                date = OffsetDateTime.now(),
                owner = "James Bond",
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(20),
                date = OffsetDateTime.now(),
            ),
        )

        // When
        val result = reducer.reduce(initEvent, events)

        // Then
        result.owner shouldBe "James Bond"
        result.maintenanceCount shouldBe 0
        result.sequenceId shouldBe SequenceId(10)
    }

    @Test
    fun `keeps the last handler registered for an event type`() {
        // Given
        val reducer = EventsReducer<CarProjection, CarEvent, CarCreated>(
            factory = CarProjection::applyInit,
        )
            .register<CarPurchased>(CarProjection::apply)
            .register(CarPurchased::class) { projection, _ -> projection.copy(owner = "Q") }

        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Aston Martin DB5",
        )
        val events = listOf(
            CarPurchased(
                id = initEvent.id,
                sequenceId = SequenceId(10),
                date = OffsetDateTime.now(),
                owner = "James Bond",
            ),
        )

        // When
        val result = reducer.reduce(initEvent, events)

        // Then
        result.owner shouldBe "Q"
    }
}
