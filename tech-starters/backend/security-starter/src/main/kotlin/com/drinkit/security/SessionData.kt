package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Values kept in the session of the connected user, to avoid computing them again at each request. They are gone at
 * logout, and kept when the session id changes.
 */
@TechStarterTool
interface SessionData {

    /** The value stored under [key], or null when none is stored or the stored one can no longer be read. */
    operator fun <T : Any> get(key: SessionKey<T>): T?

    /** Stores [value] under [key]. An anonymous caller has no session to store it in. */
    operator fun <T : Any> set(key: SessionKey<T>, value: T)

    fun remove(key: SessionKey<*>)

    fun <T : Any> getOrPut(key: SessionKey<T>, compute: () -> T): T = get(key) ?: compute().also { set(key, it) }
}

/**
 * A value kept in the session of the connected user, under its stable [name]. Built by [sessionKey].
 *
 * The key holds the full type of its value, generics included, for the value to be read back as it was written.
 */
@TechStarterTool
class SessionKey<T : Any>
    @PublishedApi
    internal constructor(val name: String, internal val type: KType)

/** Builds the key of a value kept in the session under [name]. */
@TechStarterTool
inline fun <reified T : Any> sessionKey(name: String): SessionKey<T> = SessionKey(name, typeOf<T>())
