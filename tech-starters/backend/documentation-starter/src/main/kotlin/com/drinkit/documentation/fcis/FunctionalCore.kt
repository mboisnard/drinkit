package com.drinkit.documentation.fcis

/**
 * Marks a class or function as part of the **Functional Core** of the application.
 * <p>
 * This annotation designates code that is purely functional, containing the core business logic,
 * calculations, and decision-making processes. The Functional Core is the brain of the application,
 * completely decoupled from the outside world.
 *
 * ## Principles of the Functional Core
 *
 * A class or function marked with{@code @FunctionalCore} must adhere to strict principles:
 *
 * - **Purity:** Functions should be pure. Given the same input, they must always return the
 * same output without any observable side effects.
 * - **No Side Effects:** It must not perform any I/O operations. This includes but is not
 * limited to: network requests, database access, logging, file system reads/writes, or system
 * clock access.
 * - **Immutability:** It should operate on immutable data structures. Instead of modifying
 * input data, it returns new data structures with the updated state.
 * - **Honesty:** The function's signature tells the whole story. It takes data in and returns
 * data out, with no hidden dependencies on external state or services.
 *
 * ## Role in Architecture
 *
 * The [FunctionalCore] receives data from the [ImperativeShell], processes it, and returns the
 * result. This separation makes the core business logic extremely easy to test, reason about,
 * and reuse, as it has no dependencies on infrastructure or frameworks.
 *
 * By using this annotation, you are committing to keeping this piece of code clean, predictable,
 * and free from the complexities of the outside world.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class FunctionalCore
