package com.drinkit.configuration.infra

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations

class InMemoryConfigurations : Configurations {

    private val values = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: ConfigurationKey<T>): T = if (key.name in values) values[key.name] as T else key.default

    override fun <T> set(key: ConfigurationKey<T>, value: T) {
        values[key.name] = value
    }

    override fun reset(key: ConfigurationKey<*>) {
        values.remove(key.name)
    }
}
