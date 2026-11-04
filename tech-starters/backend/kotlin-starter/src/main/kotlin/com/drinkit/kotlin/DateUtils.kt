package com.drinkit.kotlin

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.LocalDate

@TechStarterTool
fun LocalDate.isStrictlyBetween(min: LocalDate, max: LocalDate): Boolean = this.isAfter(min) && this.isBefore(max)
