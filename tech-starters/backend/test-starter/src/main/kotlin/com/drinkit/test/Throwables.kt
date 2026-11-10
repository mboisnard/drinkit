package com.drinkit.test

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
fun Throwable.rootCause(): Throwable = generateSequence(this) { it.cause }.last()
