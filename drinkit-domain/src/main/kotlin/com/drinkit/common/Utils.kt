package com.drinkit.common

import java.math.BigDecimal
import java.util.Optional

fun <T : Any> T?.toOptional(): Optional<T> =
    Optional.ofNullable(this)

fun BigDecimal.isBetween(min: Double, max: Double): Boolean =
    this.isBetween(BigDecimal.valueOf(min), BigDecimal.valueOf(max))

fun BigDecimal.isBetween(min: BigDecimal, max: BigDecimal): Boolean =
    this in min..max