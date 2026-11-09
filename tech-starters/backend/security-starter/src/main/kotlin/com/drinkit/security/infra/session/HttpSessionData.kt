package com.drinkit.security.infra.session

import com.drinkit.security.SessionData
import com.drinkit.security.SessionKey
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import kotlin.reflect.jvm.javaType

private const val ATTRIBUTE_PREFIX = "starters.security.session."

/**
 * Each value is stored as a JSON text, read back with the type of its key. A value that can no longer be read, after
 * its type changed, is absent rather than an error.
 */
internal class HttpSessionData(private val jsonMapper: JsonMapper) : SessionData {

    override fun <T : Any> get(key: SessionKey<T>): T? {
        val json = request().getSession(false)?.getAttribute(key.attribute) as? String ?: return null
        return try {
            jsonMapper.readValue(json, jsonMapper.typeFactory.constructType(key.type.javaType))
        } catch (_: JacksonException) {
            null
        }
    }

    override fun <T : Any> set(key: SessionKey<T>, value: T) {
        request().getSession(true).setAttribute(key.attribute, jsonMapper.writeValueAsString(value))
    }

    override fun remove(key: SessionKey<*>) {
        request().getSession(false)?.removeAttribute(key.attribute)
    }

    private fun request() = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

    private val SessionKey<*>.attribute get() = ATTRIBUTE_PREFIX + name
}
