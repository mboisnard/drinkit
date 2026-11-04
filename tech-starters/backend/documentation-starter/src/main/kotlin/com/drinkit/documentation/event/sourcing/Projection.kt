package com.drinkit.documentation.event.sourcing

import com.drinkit.documentation.tech.starter.TechStarterTool

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
 * - **Event Consumption:** A Projection folds the history of events of an aggregate. It reads the
 * events it cares about and ignores the rest.
 * - **Building Read Models:** When a relevant event occurs, the Projection updates its own
 * data store. This store is a denormalized representation of the data, tailored for a specific
 * use case.
 * - **Decoupling:** Projections are completely decoupled from the write model. You can add,
 * remove, or rebuild projections without impacting the [Aggregate]s in any way.
 * - **Same Transaction:** DrinkIt folds the projection in the transaction that appends the event,
 * so a read always sees the last write, never an eventually consistent view.
 *
 * By annotating a class with [Projection], you identify it as a consumer of events responsible
 * for creating a queryable view of the application's state.
 */
@TechStarterTool
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Projection
