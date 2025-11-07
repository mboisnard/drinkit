package com.drinkit.documentation.clean.architecture

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CoreDomain(
    val name: String = "",
    val description: String = "",
)
