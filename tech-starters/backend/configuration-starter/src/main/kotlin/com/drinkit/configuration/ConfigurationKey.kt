package com.drinkit.configuration

/**
 * Type-safe key for accessing configuration values.
 *
 * Each key is parameterized with its value type [T]. The key name is automatically derived
 * from the class name in kebab-case format (e.g., `MaintenanceMode` becomes `maintenance-mode`).
 *
 * Supports primitives (Int, Boolean, String, etc.) and complex types (data classes).
 *
 * Example:
 * ```kotlin
 * object AppConfig {
 *     object MaintenanceMode : ConfigurationKey<Boolean>
 *     object MaxRetries : ConfigurationKey<Int>
 * }
 * ```
 */
interface ConfigurationKey<T> {

    val key: String
        get() = this::class.simpleName!!.replace(Regex("(?<=[a-z])(?=[A-Z])"), "-").lowercase()
}
