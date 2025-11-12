package com.drinkit.documentation.tech.starter

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TechStarterTool(
    val name: String = "",
    val description: String = "",
)
