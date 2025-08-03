package com.drinkit.common

import com.drinkit.utils.addIfNotMatch

data class Percentage(val value: Double) {
    fun validate() = buildList {
        addIfNotMatch(value >= 0, "Must be positive or zero, value: $value")
        addIfNotMatch(value <= 100, "Must be less than or equals to 100, value: $value")
    }
}