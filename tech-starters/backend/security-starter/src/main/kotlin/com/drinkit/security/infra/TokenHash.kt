package com.drinkit.security.infra

import java.security.MessageDigest
import java.util.HexFormat

/**
 * The SHA-256 of [token] in hexadecimal: what the starter stores of a token or a code, so that a stolen table opens
 * no account. A token of 256 random bits cannot be found back from its hash.
 *
 * OWASP: Session Management Cheat Sheet (tokens stored as hashes).
 */
internal fun hashOf(token: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

/** Compares two hashes in constant time, so that the time of a refusal tells nothing of how much of them matched. */
internal fun String.matchesHash(hash: String) = MessageDigest.isEqual(toByteArray(), hash.toByteArray())
