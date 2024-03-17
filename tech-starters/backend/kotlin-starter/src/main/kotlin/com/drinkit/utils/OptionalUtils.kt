package com.drinkit.utils

import java.util.*

fun <T : Any> T?.toOptional(): Optional<T> =
    Optional.ofNullable(this)

fun <T> Optional<T>.orNull(): T? = this.orElse(null)
