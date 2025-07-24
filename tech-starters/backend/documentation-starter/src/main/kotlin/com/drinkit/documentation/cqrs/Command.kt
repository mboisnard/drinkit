package com.drinkit.documentation.cqrs

/**
 * Marks a class as a **Command** in a CQRS (Command Query Responsibility Segregation) architecture.
 * <p>
 * A Command represents a clear and explicit intent to change the state of the application. It is an
 * imperative message that encapsulates all the necessary information to perform an action.
 *
 * ## Principles of a Command
 *
 * A class marked with {@code @Command} is a simple data-carrying object with specific characteristics:
 * <ul>
 * <li><b>Intent-Based:</b> Its name should clearly describe the action to be performed, typically
 * using an imperative verb (e.g., {@code CreateUserCommand}).</li>
 * <li><b>State-Changing:</b> The sole purpose of processing a Command is to mutate the application's
 * state. It is handled by the "write model" of the application.</li>
 * <li><b>Self-Contained:</b> It contains all the data required for its handler to execute the
 * requested change.</li>
 * </ul>
 *
 * ## Role in Architecture
 *
 * In CQRS, the write side of the application is separated from the read side. Commands are the only
 * way to trigger changes in the system.
 *
 * Using this annotation clearly separates the operations that modify data from those that only read it.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Command
