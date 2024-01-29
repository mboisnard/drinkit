package com.drinkit.utils

import java.time.LocalDate

fun LocalDate.isBetween(min: LocalDate, max: LocalDate): Boolean =
    this.isAfter(min) && this.isBefore(max)