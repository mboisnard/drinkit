package com.drinkit.utils

fun <T> MutableList<T>.addIfNotMatch(predicate: Boolean, value: T) {
    if (!predicate) {
        add(value)
    }
}