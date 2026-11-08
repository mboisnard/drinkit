package com.drinkit.security.infra

import com.drinkit.kotlin.sha256
import java.security.MessageDigest

/** OWASP: Session Management Cheat Sheet (tokens stored as hashes). */
internal fun hashOf(token: String): String = token.sha256()

internal fun String.matchesHash(hash: String) = MessageDigest.isEqual(toByteArray(), hash.toByteArray())
