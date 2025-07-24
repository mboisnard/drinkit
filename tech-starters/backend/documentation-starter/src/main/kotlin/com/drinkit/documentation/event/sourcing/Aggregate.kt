package com.drinkit.documentation.event.sourcing

/**
 * Marks a class or function as a component of an **Aggregate**, the core element of the write model
 * in Domain-Driven Design (DDD) and Event Sourcing.
 * <p>
 * An Aggregate is a cluster of associated objects that we treat as a single unit for the purpose
 * of data changes. It acts as a transactional boundary and is the ultimate guardian of its
 * business invariants (rules that must always be true).
 *
 * ## Role in Event Sourcing
 *
 * In an Event Sourcing context, the Aggregate's role is to process incoming {@link Command}s and
 * produce a sequence of {@link Event}s as a result.
 * <ul>
 * <li><b>Command Processing:</b> It is the sole entry point for any modification. A command handler
 * method within the Aggregate decides whether the requested action is valid based on its
 * current state.</li>
 * <li><b>Event Production:</b> If a command is valid, the Aggregate doesn't change its state
 * directly. Instead, it produces one or more events that describe what happened. These events
 * are the single source of truth.</li>
 * <li><b>State Reconstruction:</b> The Aggregate's state is never stored directly. It is always
 * rebuilt by replaying its own historical stream of events from the beginning. It contains
 * logic to apply each event to itself to reach its current state.</li>
 * </ul>
 *
 * This annotation identifies the primary consistency boundary for all write operations.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Aggregate
