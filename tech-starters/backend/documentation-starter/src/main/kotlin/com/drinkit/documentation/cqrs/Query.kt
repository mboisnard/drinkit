package com.drinkit.documentation.cqrs

/**
 * Marks a class as a **Query** in a CQRS (Command Query Responsibility Segregation) architecture.
 *
 * A Query is a request for information from the application. It is a declarative message used to
 * retrieve data without altering the state of the system in any way.
 *
 * ## Principles of a Query
 *
 * A class marked with `@Query` is a data request object with strict rules:
 *
 * - **Side-Effect Free:** A Query and its handler **must not** change the state of the application.
 * It is a completely read-only operation. This is the fundamental guarantee of the pattern.
 * - **Data Retrieval:** Its only purpose is to fetch data.
 * - **Tailored Results:** The result of a Query is typically a DTO (Data Transfer Object)
 * specifically designed for the needs of the consumer. This prevents over-fetching and
 * complex data mapping on the client side.
 *
 * ## Role in Architecture
 *
 * In CQRS, Queries are handled independently of [Command]s. This separation allows the read
 * model to be optimized for performance, using different database technologies or data structures
 * than the write model.
 *
 * This annotation helps enforce the strict separation between state-modifying and data-retrieving
 * operations, which is the core benefit of CQRS.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Query
