package com.drinkit.configuration

import com.drinkit.documentation.tech.starter.TechStarterTool

/**
 * The settings stored in the database, read and written by their [ConfigurationKey].
 */
@TechStarterTool
interface Configurations {

    /** The stored value of the key, or its default when none is stored or the stored one cannot be read. */
    operator fun <T> get(key: ConfigurationKey<T>): T

    operator fun <T> set(key: ConfigurationKey<T>, value: T)

    /** Removes the stored value: the key reads its default again. */
    fun reset(key: ConfigurationKey<*>)
}
