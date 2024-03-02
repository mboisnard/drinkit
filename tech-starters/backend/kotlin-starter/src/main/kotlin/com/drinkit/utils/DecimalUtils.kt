package com.drinkit.utils

import java.math.BigDecimal

fun BigDecimal.isBetween(min: Double, max: Double): Boolean =
    this.isBetween(BigDecimal.valueOf(min), BigDecimal.valueOf(max))

fun BigDecimal.isBetween(min: BigDecimal, max: BigDecimal): Boolean =
    this in min..max