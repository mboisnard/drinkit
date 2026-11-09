package com.drinkit.event.sourcing

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KClass

/**
 * An event reducer using a non-empty list design to build value object projections.
 *
 * The [factory] is used to handle the initial event, it produces the initial
 * state of the projection. The derived state is used as the seed on which
 * to apply the rest of the events.
 *
 * The remaining events are handled by folding functions that take the current
 * state of the projection, the current event, and return an updated state to
 * be used for the next event.
 *
 * You can [register] an event-specific folding function. An event with no
 * registered function goes to the [defaultHandler], which leaves the
 * projection as it is unless you pass your own.
 *
 * If you register multiple specific handlers for the same class only the last
 * registered handler will be used.
 *
 */
@TechStarterTool
class EventsReducer<Projection, Event : DomainEvent, InitialEvent : Event>(
    private val factory: (InitialEvent) -> Projection,
    private val defaultHandler: (Projection, Event) -> Projection = { projection, _ -> projection },
) {
    private var handlers: Map<KClass<out Event>, (Projection, Event) -> Projection> = emptyMap()

    fun <T : Event> register(
        klass: KClass<T>,
        handler: (Projection, T) -> Projection,
    ): EventsReducer<Projection, Event, InitialEvent> {
        @Suppress("UNCHECKED_CAST")
        handlers = handlers + (klass to handler as (Projection, Event) -> Projection)
        return this
    }

    /**
     * Register a handler for the given event type using a method reference.
     * This allows for simplified syntax like: register<EventType>(Projection::methodName)
     */
    inline fun <reified T : Event> register(
        noinline handler: Projection.(T) -> Projection,
    ): EventsReducer<Projection, Event, InitialEvent> = register(
        T::class,
    ) { projection, event -> projection.handler(event) }

    private fun apply(projection: Projection, event: Event): Projection {
        val handler = handlers[event::class] ?: defaultHandler
        return handler(projection, event)
    }

    fun reduce(initialEvent: InitialEvent, events: List<Event>): Projection {
        val seed = factory(initialEvent)
        return events.fold(seed, ::apply)
    }

    fun reduce(history: History<Event, InitialEvent>): Projection = reduce(history.initEvent, history.remainingEvents)
}
