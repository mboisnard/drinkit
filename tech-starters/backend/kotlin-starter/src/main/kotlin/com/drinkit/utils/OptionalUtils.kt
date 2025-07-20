package com.drinkit.utils

import java.util.*

fun <T : Any> T?.toOptional(): Optional<T> =
    Optional.ofNullable(this)

val <T> Optional<T>.orNull get(): T? = this.orElse(null)
