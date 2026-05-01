package com.drinkit.featureflags

import com.drinkit.documentation.tech.starter.TechStarterTool

data class FeatureFlagContext(
    val userId: String? = null,
    val sessionId: String? = null,
    val correlationId: String? = null,
    val email: String? = null,
    val admin: Boolean = false,
    val attributes: Map<String, String> = emptyMap(),
)

@TechStarterTool
interface FeatureFlags {
    fun isEnabled(flag: String): Boolean
    fun isEnabled(flag: String, context: FeatureFlagContext): Boolean

    fun isDisabled(flag: String): Boolean = !isEnabled(flag)

    fun isEnabledForUser(flag: String, userId: String): Boolean =
        isEnabled(flag, FeatureFlagContext(userId = userId))
}

const val USER_ID_ATTRIBUTE_KEY = "userId"
const val EMAIL_ATTRIBUTE_KEY = "email"
const val ADMIN_ATTRIBUTE_KEY = "admin"
const val CORRELATION_ID_ATTRIBUTE_KEY = "correlationId"