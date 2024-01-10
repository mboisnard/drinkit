package com.drinkit.common

import java.util.Optional

fun <T : Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)