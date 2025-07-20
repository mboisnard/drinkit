package com.drinkit.messaging

/**
 * Defines the base contract for a platform-level event, using the
 * Curiously Recurring Generic Pattern (CRGP).
 *
 * This pattern enforces a strong, self-referential type relationship. Any class
 * implementing this interface must use itself as the generic `Type` parameter
 * (e.g., `class UserCreatedEvent : PlatformEvent<UserCreatedEvent>`).
 *
 * The primary benefit is that methods defined within this interface can return the
 * specific subtype (`Type`), enabling fluent, type-safe APIs without the need for casting.
 *
 * @param <Type> The concrete event class that implements this interface.
 */
interface PlatformEvent<Type : PlatformEvent<Type>>
