package com.drinkit.kotlin

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.*

@TechStarterTool
fun <T : Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

@TechStarterTool
val <T> Optional<T>.orNull get(): T? = this.orElse(null)
