package com.drinkit.common

import com.drinkit.kotlin.addUnless

data class Percentage(val value: Double) {
    fun validate() = buildList {
        addUnless(value >= 0, "Must be positive or zero, value: $value")
        addUnless(value <= 100, "Must be less than or equals to 100, value: $value")
    }
}
