package com.drinkit.configuration.infra

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations
import kotlin.reflect.KClass

class InMemoryConfigurations : Configurations {

    private val configurations = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: ConfigurationKey<T>, type: KClass<T>): T? =
        configurations[key.key] as? T

    override fun <T : Any> set(key: ConfigurationKey<T>, value: T, type: KClass<T>): T {
        configurations[key.key] = value
        return value
    }

    override fun delete(key: ConfigurationKey<*>) {
        configurations.remove(key.key)
    }
}