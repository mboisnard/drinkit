package com.drinkit.kotlin

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.math.BigDecimal
import java.math.RoundingMode

val SAFE_ROUNDING_MODE = RoundingMode.HALF_EVEN

@TechStarterTool
fun BigDecimal.isBetween(min: Double, max: Double): Boolean =
    this.isBetween(BigDecimal.valueOf(min), BigDecimal.valueOf(max))

@TechStarterTool
fun BigDecimal.isBetween(min: BigDecimal, max: BigDecimal): Boolean = this in min..max

@TechStarterTool
fun BigDecimal.safelyDivide(divisor: BigDecimal, scale: Int): BigDecimal =
    this.divide(divisor, scale, SAFE_ROUNDING_MODE)
