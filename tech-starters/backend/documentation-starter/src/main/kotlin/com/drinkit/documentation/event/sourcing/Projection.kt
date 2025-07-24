package com.drinkit.documentation.event.sourcing

/**
 * Marks a class as a **Projection**, also known as a **Read Model**, in an Event Sourcing architecture.
 * <p>
 * A Projection's responsibility is to consume a stream of events and build a persistent,
 * optimized view of data. This view is specifically designed to efficiently answer the application's
 * {@link Query} requests.
 *
 * ## Role and Responsibility
 *
 * While the {@link Aggregate} is the authority for writes, Projections are the workhorses for reads.
 * <ul>
 * <li><b>Event Consumption:</b> A Projection subscribes to an event stream. It listens for specific
 * events it cares about and ignores the rest.</li>
 * <li><b>Building Read Models:</b> When a relevant event occurs, the Projection updates its own
 * data store. This store is a denormalized representation of the data, tailored for a specific
 * use case.</li>
 * <li><b>Decoupling:</b> Projections are completely decoupled from the write model. You can add,
 * remove, or rebuild projections without impacting the {@link Aggregate}s in any way.</li>
 * <li><b>Eventual Consistency:</b> Read models are updated asynchronously as events are processed.
 * This means they are eventually consistent with the write model.</li> <-- TODO Review for sync
 * </ul>
 *
 * By annotating a class with {@code @Projection}, you identify it as a consumer of events responsible
 * for creating a queryable view of the application's state.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Projection
