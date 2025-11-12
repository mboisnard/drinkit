package com.drinkit.configuration

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KClass

/**
 * Service for managing application configurations with type-safe keys.
 *
 * Provides CRUD operations for configuration values using [ConfigurationKey] for type safety.
 * Serialization and deserialization are handled automatically based on the key's type.
 *
 * Example:
 * ```kotlin
 * object AppConfig {
 *     object MaxRetries : ConfigurationKey<Int>
 * }
 *
 * configurations.set(AppConfig.MaxRetries, 3)
 * val retries = configurations.get(AppConfig.MaxRetries) // returns 3
 * ```
 */
@TechStarterTool
interface Configurations {

    fun <T : Any> get(key: ConfigurationKey<T>, type: KClass<T>): T?

    fun <T : Any> getOrSetDefault(key: ConfigurationKey<T>, default: T, type: KClass<T>): T =
        get(key, type) ?: set(key, default, type)

    fun <T : Any> getOrThrow(key: ConfigurationKey<T>, type: KClass<T>): T =
        get(key, type) ?: throw NoSuchElementException("Configuration not found for key: ${key.key}")

    fun <T : Any> set(key: ConfigurationKey<T>, value: T, type: KClass<T>): T

    fun delete(key: ConfigurationKey<*>)
}

// Extension functions with reified type parameters for easier usage and avoid passing KClass objects

inline fun <reified T : Any> Configurations.get(key: ConfigurationKey<T>): T? =
    get(key, T::class)

inline fun <reified T : Any> Configurations.getOrSetDefault(key: ConfigurationKey<T>, default: T): T =
    getOrSetDefault(key, default, T::class)

inline fun <reified T : Any> Configurations.getOrThrow(key: ConfigurationKey<T>): T =
    getOrThrow(key, T::class)

inline fun <reified T : Any> Configurations.set(key: ConfigurationKey<T>, value: T) =
    set(key, value, T::class)
