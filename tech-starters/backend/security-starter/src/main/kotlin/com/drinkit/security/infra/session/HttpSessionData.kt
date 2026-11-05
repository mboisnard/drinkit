package com.drinkit.security.infra.session

import com.drinkit.security.CurrentUser
import com.drinkit.security.SessionData
import com.drinkit.security.SessionKey
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import kotlin.reflect.jvm.javaType

private const val ATTRIBUTE_PREFIX = "starters.security.session."

/**
 * Keeps the [SessionData] of the connected user in the HTTP session, so that the application computes them once per
 * session. Each value is stored as a JSON text: the session store reads a text back whatever the type of the value,
 * and this class reads the value back with the type of its key. A value that can no longer be read, after its type
 * changed, is absent rather than an error, and an anonymous caller gets no session to store values in.
 *
 * OWASP: ASVS 5.0 V1.5.2, Deserialization Cheat Sheet (typed JSON, no Java serialization).
 */
internal class HttpSessionData(private val currentUser: CurrentUser, private val jsonMapper: JsonMapper) : SessionData {

    override fun <T : Any> get(key: SessionKey<T>): T? {
        val json = request().getSession(false)?.getAttribute(key.attribute) as? String ?: return null
        return try {
            jsonMapper.readValue(json, jsonMapper.typeFactory.constructType(key.type.javaType))
        } catch (_: JacksonException) {
            null
        }
    }

    override fun <T : Any> set(key: SessionKey<T>, value: T) {
        currentUser.orNull() ?: throw AuthenticationCredentialsNotFoundException("An anonymous caller has no session")
        request().getSession(true).setAttribute(key.attribute, jsonMapper.writeValueAsString(value))
    }

    override fun remove(key: SessionKey<*>) {
        request().getSession(false)?.removeAttribute(key.attribute)
    }

    private fun request() = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

    private val SessionKey<*>.attribute get() = ATTRIBUTE_PREFIX + name
}
