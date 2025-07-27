package com.drinkit.documentation.clean.architecture

/**
 * Marks a class as a **Use Case**, often called an **Interactor**, which is a core component
 * in a Clean Architecture design.
 *
 * This annotation signifies that the class encapsulates a specific,
 * self-contained business operation or user-initiated action.
 *
 * ## Role and Responsibility
 *
 * A Use Case's primary responsibility is to execute a single, well-defined piece of business logic.
 * It acts as an orchestrator, directing the flow of data and coordinating interactions between
 * business objects (Entities) and data sources (Repositories).
 *
 * ## Implementation
 *
 * Typically, a Use Case exposes a single public method (e.g., `execute()` or `invoke()`) that
 * receives input data (as a simple request model) and returns output data (as a simple response model).
 * It relies on abstractions (interfaces) for any external dependencies, such as fetching data from a
 * repository, which are injected into its constructor.
 *
 *
 * By using this annotation, you are explicitly marking it as a foundational
 * piece of your application's business logic, adhering to the principles of separation of concerns
 * and dependency inversion.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Usecase
