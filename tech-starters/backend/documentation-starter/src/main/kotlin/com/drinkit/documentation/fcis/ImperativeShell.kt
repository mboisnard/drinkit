package com.drinkit.documentation.fcis

/**
 * Marks a class as part of the **Imperative Shell** of the application.
 *
 * This annotation identifies a component that interacts with the outside world and manages all
 * side effects. The Imperative Shell acts as a boundary layer, connecting the pure
 * [FunctionalCore] to the messy, unpredictable reality of infrastructure.
 *
 * ## Role and Responsibility
 *
 * The Imperative Shell is responsible for all "impure" actions. Its job is to:
 *
 * - **Handle I/O:** Perform all communication with external systems, such as making HTTP
 * requests, querying a database, reading from files, or interacting with third-party APIs.
 * - **Manage State:** Persist and retrieve state from database.
 * - **Orchestrate Logic:** Prepare the data required by the Functional Core, invoke one or
 * more core functions to execute the business logic, and then process the results.
 * - **Dependency Management:** Handle dependency injection, configuration, and the lifecycle
 * of infrastructure components.
 *
 * ## Relationship with the Functional Core
 *
 * The Shell is the only part of the application that should call the Core. It translates external
 * events into calls to pure functions and translates the results of those calls back into actions
 * in the outside world. This keeps the core logic clean and testable.
 *
 *
 * A class annotated with [ImperativeShell] is explicitly acknowledged as being stateful and
 * impure, forming the robust outer layer of the application.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ImperativeShell
