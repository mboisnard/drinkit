package com.drinkit.kotlin

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
fun <T> MutableList<T>.addUnless(condition: Boolean, value: T) {
    if (!condition) {
        add(value)
    }
}
