package com.drinkit.event.sourcing

import kotlin.reflect.KClass

/**
 * An event reducer using a non-empty list design to build value
 * object projections.
 *
 * The [factory] is used to handle the initial event, it produces the initial
 * state of the projection. The derived state is used as the seed on which
 * to apply the rest of the events.
 *
 * The remaining events are handled by folding functions that take the current
 * state of the projection, the current event, and return an updated state to
 * be used for the next event.
 *
 * You can [register] an event-specific folding function, if no specific
 * function has been registered, the event will be handled by the mandatory
 * [defaultHandler].
 *
 * If you register multiple specific handlers for the same class only the last
 * registered handler will be used.
 *
 */
class EventsReducer<Projection, Event : Any, InitialEvent : Event>(
    private val factory: (InitialEvent) -> Projection,
    private val defaultHandler: (Projection, Event) -> Projection = {projection, _ -> projection },
) {
    private var handlers: Map<KClass<out Event>, (Projection, Event) -> Projection> = emptyMap()

    /**
     * Register a handler for the given event type.
     */
    fun <T : Event> register(
        klass: KClass<T>,
        handler: (Projection, T) -> Projection,
    ): EventsReducer<Projection, Event, InitialEvent> {
        @Suppress("UNCHECKED_CAST")
        handlers = handlers + (klass to handler as (Projection, Event) -> Projection)
        return this
    }

    /**
     * Register a handler for the given event type.
     */
    inline fun <reified T : Event> register(
            noinline handler: (Projection, T) -> Projection,
    ): EventsReducer<Projection, Event, InitialEvent> = register(T::class, handler)

    private fun apply(projection: Projection, event: Event): Projection {
        val handler = handlers[event::class] ?: defaultHandler
        return handler(projection, event)
    }

    fun reduce(initialEvent: InitialEvent, events: List<Event>): Projection {
        val seed = factory(initialEvent)
        return events.fold(seed, ::apply)
    }
}
