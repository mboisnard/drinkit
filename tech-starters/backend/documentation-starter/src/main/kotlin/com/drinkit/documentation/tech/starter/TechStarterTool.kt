package com.drinkit.documentation.tech.starter

@TechStarterTool
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class TechStarterTool(val name: String = "", val description: String = "")
