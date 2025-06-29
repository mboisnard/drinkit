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
    fun `should build a projection from a sequence of events`() {
        // Given
        val reducer = EventsReducer<CarProjection, CarEvent, CarCreated>(
            factory = CarProjection::applyInit
        )
            .register<CarPurchased>(CarProjection::apply)
            .register<MaintenanceCarriedOut>(CarProjection::apply)

        val initEvent = CarCreated(
            id = UUID.randomUUID(),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(),
            name = "Aston Martin DB5"
        )
        val events = listOf(
            CarPurchased(
                id = initEvent.id,
                sequenceId = SequenceId(10),
                date = OffsetDateTime.now(),
                owner = "James Bond"
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(20),
                date = OffsetDateTime.now()
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(30),
                date = OffsetDateTime.now()
            )
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
    fun `should use default handler for unregistered event types`() {
        // Given
        val reducer = EventsReducer<CarProjection, CarEvent, CarCreated>(
            factory = CarProjection::applyInit,
            defaultHandler = { projection, event -> projection.copy(sequenceId = event.sequenceId) }
        )
            // Can also be written with .register(CarPurchased::class) { projection, event -> projection.apply(event) } or .register<CarPurchased>(CarProjection::apply)
            .register(CarPurchased::class, CarProjection::apply)


        val initEvent = CarCreated(
                id = UUID.randomUUID(),
                sequenceId = SequenceId(),
                date = OffsetDateTime.now(),
                name = "Aston Martin DB5"
        )
        val events = listOf(
            CarPurchased(
                id = initEvent.id,
                sequenceId = SequenceId(10),
                date = OffsetDateTime.now(),
                owner = "James Bond"
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(20),
                date = OffsetDateTime.now()
            ),
            MaintenanceCarriedOut(
                id = initEvent.id,
                sequenceId = SequenceId(30),
                date = OffsetDateTime.now()
            )
        )

        // When

        // Reduce the events to build the final state, include MaintenanceCarriedOut which doesn't have a registered handler
        val result = reducer.reduce(initEvent, events)

        // Then
        result.id shouldBe initEvent.id
        result.name shouldBe initEvent.name
        result.owner shouldBe "James Bond"
        result.maintenanceCount shouldBe 0 // Still 0 because MaintenanceCarriedOut was handled by the default handler
        result.sequenceId shouldBe SequenceId(30)
    }
}

/**
 * Base interface to represent any event that can be applied to a CarProjection.
 */
sealed interface CarEvent {
    val id: CarId
    val sequenceId: SequenceId
    val date: OffsetDateTime
}

/**
 * Initial event that starts the car history
 */
data class CarCreated(
    override val id: CarId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
    val name: String
) : CarEvent

data class CarPurchased(
    override val id: CarId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
    val owner: String
) : CarEvent

data class MaintenanceCarriedOut(
    override val id: CarId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
) : CarEvent

typealias CarId = UUID
data class CarProjection(
    val id: CarId,
    val name: String,
    val owner: String?,
    val sequenceId: SequenceId,
    val maintenanceCount: Int = 0,
) {
    companion object {
        fun applyInit(initEvent: CarCreated): CarProjection =
            CarProjection(
                id = initEvent.id,
                name = initEvent.name,
                owner = null,
                sequenceId = initEvent.sequenceId
            )
    }

    fun apply(event: CarPurchased) = copy(owner = event.owner, sequenceId = event.sequenceId)

    fun apply(event: MaintenanceCarriedOut) = copy(maintenanceCount = maintenanceCount + 1, sequenceId = event.sequenceId)
}
