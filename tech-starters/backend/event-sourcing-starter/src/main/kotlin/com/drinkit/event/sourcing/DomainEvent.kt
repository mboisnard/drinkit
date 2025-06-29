package com.drinkit.event.sourcing

/**
 * Interface representing a domain event in an event-sourced system.
 * All domain events must have a sequenceId to maintain order.
 */
interface DomainEvent {
    val sequenceId: SequenceId
}

/**
 * Represents a chronological sequence of domain events, starting with an initial event.
 *
 * This class is useful for:
 * - Maintaining the complete history of an entity
 * - Ensuring there is always an initial event
 * - Ensuring events are processed in the correct order
 * - Providing a clean separation between the initial creation event and subsequent events
 *
 */
data class History<Event : DomainEvent, InitEvent: Event>(
    val initEvent: InitEvent,
    val remainingEvents: List<Event> = emptyList(),
) {

    companion object {
        inline fun <Event : DomainEvent, reified InitEvent : Event> from(events: List<Event>): History<Event, InitEvent> {
            val initEvent = events.firstOrNull()
                ?: throw IllegalArgumentException("Events list cannot be empty")

            require(initEvent is InitEvent) {
                "First event must be of type ${InitEvent::class.simpleName}, but was ${initEvent::class.simpleName}"
            }

            require(events.sortedBy { it.sequenceId } == events) {
                "Events should be sequentially sorted: $events"
            }

            return History(initEvent, events.drop(1))
        }
    }
}