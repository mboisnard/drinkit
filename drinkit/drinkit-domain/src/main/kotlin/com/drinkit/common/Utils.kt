package com.drinkit.common

import com.drinkit.common.Constants.ID_REGEX
import com.drinkit.common.Constants.INVISIBLE_CHARS_REGEX
import com.drinkit.common.Constants.SPECIAL_CHAR_REGEX
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.Optional

fun <T : Any> T?.toOptional(): Optional<T> =
    Optional.ofNullable(this)

fun <T> Optional<T>.orNull(): T? = this.orElse(null)

fun BigDecimal.isBetween(min: Double, max: Double): Boolean =
    this.isBetween(BigDecimal.valueOf(min), BigDecimal.valueOf(max))

fun BigDecimal.isBetween(min: BigDecimal, max: BigDecimal): Boolean =
    this in min..max

fun String.containsInvisibleCharacters(): Boolean =
    INVISIBLE_CHARS_REGEX.containsMatchIn(this)

fun String.hasMinLength(min: Int): Boolean =
    this.length >= min

fun String.hasLengthBetween(min: Int, max: Int): Boolean =
    this.length in min..max

fun String.containsACapitalLetter(): Boolean =
    this.any { it.isUpperCase() }

fun String.containsANumber(): Boolean =
    this.any { it.isDigit() }

fun String.isId(): Boolean =
    ID_REGEX.matches(this)

fun String.containsSpecialCharacter(): Boolean =
    SPECIAL_CHAR_REGEX.containsMatchIn(this)

fun LocalDate.isBetween(min: LocalDate, max: LocalDate): Boolean =
    this.isAfter(min) && this.isBefore(max)