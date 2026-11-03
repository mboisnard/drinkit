package com.drinkit.configuration

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A setting that can change without a restart: its stable [name] in the table, and the [default] read while none is
 * stored. Built by [configurationKey].
 *
 * The key holds the full type of its value, generics included, for Jackson to read a `List<Tag>` back as tags. Only an
 * inline function with a reified type gets that type from `typeOf<T>()`, so the constructor stays internal and
 * `@PublishedApi` lets that function, copied into the caller's module, still call it.
 */
@TechStarterTool
class ConfigurationKey<T>
    @PublishedApi
    internal constructor(
        val name: String,
        val default: T,
        internal val type: KType,
    )

/** Builds the key of a setting stored under [name], which reads [default] while none is stored. */
@TechStarterTool
inline fun <reified T> configurationKey(name: String, default: T): ConfigurationKey<T> =
    ConfigurationKey(name, default, typeOf<T>())
