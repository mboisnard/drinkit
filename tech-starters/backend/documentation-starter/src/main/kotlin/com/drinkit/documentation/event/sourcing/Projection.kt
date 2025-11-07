package com.drinkit.documentation.event.sourcing

/**
 * Marks a class as a **Projection**, also known as a **Read Model**, in an Event Sourcing architecture.
 *
 * A Projection's responsibility is to consume a stream of events and build a persistent,
 * optimized view of data. This view is specifically designed to efficiently answer the application's
 * [com.drinkit.documentation.cqrs.Query] requests.
 *
 * ## Role and Responsibility
 *
 * While the [Aggregate] is the authority for writes, [Projection]s are the workhorses for reads.
 *
 * - **Event Consumption:** A Projection subscribes to an event stream. It listens for specific
 * events it cares about and ignores the rest.
 * - **Building Read Models:** When a relevant event occurs, the Projection updates its own
 * data store. This store is a denormalized representation of the data, tailored for a specific
 * use case.
 * - **Decoupling:** Projections are completely decoupled from the write model. You can add,
 * remove, or rebuild projections without impacting the [Aggregate]s in any way.
 * - **Eventual Consistency:** Read models are updated asynchronously as events are processed.
 * This means they are eventually consistent with the write model. <-- TODO Review for sync
 *
 * By annotating a class with [Projection], you identify it as a consumer of events responsible
 * for creating a queryable view of the application's state.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Projection
