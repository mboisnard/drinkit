package com.drinkit.featuremanagement

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.drinkit.security.ConnectedUser

@TechStarterTool
data class FlagSubject(val key: String, val attributes: Map<String, String> = emptyMap())

/** What a flag may target about a signed-in user beyond their roles, such as their plan. */
@TechStarterTool
fun interface SubjectAttributes {
    fun of(user: ConnectedUser): Map<String, String>
}
