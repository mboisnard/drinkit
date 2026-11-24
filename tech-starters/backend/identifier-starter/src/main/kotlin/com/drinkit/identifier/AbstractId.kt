package com.drinkit.identifier

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigInteger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val CROCKFORD_BASE32 = "0123456789abcdefghjkmnpqrstvwxyz"
private const val ID_LENGTH = 26

// 26 characters of 5 bits hold 130: the first one carries the 3 highest bits of the 128 only
private val ID_FORMAT = Regex("[0-7][$CROCKFORD_BASE32]{${ID_LENGTH - 1}}")

@TechStarterTool
abstract class AbstractId(
    @get:JsonValue val value: String,
) {

    init {
        require(value.isId()) { "${javaClass.simpleName} refuses a value of another format" }
    }

    override fun equals(other: Any?) = other?.javaClass == javaClass && (other as AbstractId).value == value

    override fun hashCode() = value.hashCode()

    override fun toString() = "${javaClass.simpleName}($value)"
}

// The companion object of an identifier class extends it: CellarId.create(), CellarId.parseOrNull(input)
@TechStarterTool
open class IdFactory<T : AbstractId>(private val build: (String) -> T) {

    @OptIn(ExperimentalUuidApi::class)
    fun create(): T = build(Uuid.generateV7().toCrockford())

    fun parseOrNull(value: String): T? = value.takeIf { it.isId() }?.let(build)
}

// Readable over fast: about 800 ns an id, against 45 ns shifting the bits of two Longs, nothing next to an insert.
// typeid-java encodes it too, but would add a library for two lines.
@OptIn(ExperimentalUuidApi::class)
internal fun Uuid.toCrockford(): String = BigInteger(1, toByteArray()).toString(CROCKFORD_BASE32.length)
    .padStart(ID_LENGTH, '0')
    .map { CROCKFORD_BASE32[it.digitToInt(CROCKFORD_BASE32.length)] }
    .joinToString("")

@TechStarterTool
fun String.isId(): Boolean = ID_FORMAT.matches(this)
