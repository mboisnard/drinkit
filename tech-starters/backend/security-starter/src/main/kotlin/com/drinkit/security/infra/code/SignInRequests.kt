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
 * Read only up to 4 KiB, so that an oversized body costs no parsing.
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

/** Trimmed and in lower case, as the starter keys its codes. */
internal fun JsonNode.login(maxLength: Int): String? = path("email").stringValue("")
    .trim()
    .lowercase(Locale.ROOT)
    .takeIf { it.length <= maxLength && it.isEmail() }

internal fun JsonNode.code(): String? = path("code").stringValue("").takeIf(CODE_FORMAT::matches)
