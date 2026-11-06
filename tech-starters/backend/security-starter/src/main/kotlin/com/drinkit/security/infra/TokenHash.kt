package com.drinkit.security.infra

import java.security.MessageDigest
import java.util.HexFormat

/** OWASP: Session Management Cheat Sheet (tokens stored as hashes). */
internal fun hashOf(token: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

internal fun String.matchesHash(hash: String) = MessageDigest.isEqual(toByteArray(), hash.toByteArray())
