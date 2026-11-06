package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** Gone at logout, kept when the session id changes. */
@TechStarterTool
interface SessionData {

    operator fun <T : Any> get(key: SessionKey<T>): T?

    operator fun <T : Any> set(key: SessionKey<T>, value: T)

    fun remove(key: SessionKey<*>)

    fun <T : Any> getOrPut(key: SessionKey<T>, compute: () -> T): T = get(key) ?: compute().also { set(key, it) }
}

/** Holds the full type of its value, generics included, for the value to be read back as it was written. */
@TechStarterTool
class SessionKey<T : Any>
    @PublishedApi
    internal constructor(val name: String, internal val type: KType)

@TechStarterTool
inline fun <reified T : Any> sessionKey(name: String): SessionKey<T> = SessionKey(name, typeOf<T>())
