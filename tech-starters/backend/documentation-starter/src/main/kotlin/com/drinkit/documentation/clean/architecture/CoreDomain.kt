package com.drinkit.documentation.clean.architecture

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CoreDomain(val name: String = "", val description: String = "")
