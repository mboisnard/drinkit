package com.drinkit.documentation.tech.starter

@TechStarterTool
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TechStarterTool(val name: String = "", val description: String = "")
