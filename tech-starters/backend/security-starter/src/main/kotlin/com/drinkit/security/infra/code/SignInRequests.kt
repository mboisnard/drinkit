package com.drinkit.security.infra.code

import com.drinkit.kotlin.isEmail
import jakarta.servlet.http.HttpServletRequest
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.Locale

internal const val CODE_REQUEST_PATH = "/api/auth/code"
internal const val CODE_SIGN_IN_PATH = "/api/auth/code/sign-in"

private const val MAX_BODY_BYTES = 4096
private val CODE_FORMAT = Regex("\\d{6}")

/**
 * The JSON body of a request for a code or of a sign-in, read only up to 4 KiB, so that an oversized body costs no
 * parsing. Null when it is larger or unreadable.
 *
 * OWASP: ASVS 5.0 V2.2.1, Input Validation Cheat Sheet (size limits before parsing).
 */
internal fun HttpServletRequest.readSignInBody(jsonMapper: JsonMapper): JsonNode? {
    val bytes = inputStream.readNBytes(MAX_BODY_BYTES + 1)
    if (bytes.size > MAX_BODY_BYTES) return null

    return try {
        jsonMapper.readTree(bytes)
    } catch (_: JacksonException) {
        null
    }
}

/** The email of the body as the starter keys its codes: trimmed and in lower case, or null when it is no email. */
internal fun JsonNode.login(maxLength: Int): String? = path("email").stringValue("")
    .trim()
    .lowercase(Locale.ROOT)
    .takeIf { it.length <= maxLength && it.isEmail() }

/** The code of the body when it is six digits, null otherwise. */
internal fun JsonNode.code(): String? = path("code").stringValue("").takeIf(CODE_FORMAT::matches)
