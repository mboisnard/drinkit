package com.drinkit.event.sourcing

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Base interface to represent any event that can be applied to a CarProjection.
 */
internal sealed interface CarEvent: DomainEvent {
    val id: CarId
    val date: OffsetDateTime
}

/**
 * Initial event that starts the car history
 */
internal data class CarCreated(
    override val id: CarId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
    val name: String
) : CarEvent

internal data class CarPurchased(
    override val id: CarId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
    val owner: String
) : CarEvent

internal data class MaintenanceCarriedOut(
    override val id: CarId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
) : CarEvent

internal typealias CarId = UUID
internal data class CarProjection(
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
