package com.drinkit.documentation.event.sourcing

/**
 * Marks a class or function as a component of an **Aggregate**, the core element of the write model
 * in Domain-Driven Design (DDD) and Event Sourcing.
 *
 * An Aggregate is a cluster of associated objects that we treat as a single unit for the purpose
 * of data changes. It acts as a transactional boundary and is the ultimate guardian of its
 * business invariants (rules that must always be true).
 *
 * ## Role in Event Sourcing
 *
 * In an Event Sourcing context, the Aggregate's role is to process incoming [com.drinkit.documentation.cqrs.Command]s and
 * produce a sequence of Events as a result.
 *
 * - **Command Processing:** It is the sole entry point for any modification. A command handler
 * method within the Aggregate decides whether the requested action is valid based on its
 * current state.
 * - **Event Production:** If a command is valid, the Aggregate doesn't change its state
 * directly. Instead, it produces one or more events that describe what happened. These events
 * are the single source of truth.
 * - **State Reconstruction:** The Aggregate's state is never stored directly. It is always
 * rebuilt by replaying its own historical stream of events from the beginning. It contains
 * logic to apply each event to itself to reach its current state.
 *
 * This annotation identifies the primary consistency boundary for all write operations.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Aggregate
