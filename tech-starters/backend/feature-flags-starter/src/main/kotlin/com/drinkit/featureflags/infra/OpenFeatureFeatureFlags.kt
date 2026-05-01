package com.drinkit.featureflags.infra

import com.drinkit.featureflags.ADMIN_ATTRIBUTE_KEY
import com.drinkit.featureflags.CORRELATION_ID_ATTRIBUTE_KEY
import com.drinkit.featureflags.EMAIL_ATTRIBUTE_KEY
import com.drinkit.featureflags.FeatureFlagContext
import com.drinkit.featureflags.FeatureFlags
import com.drinkit.featureflags.USER_ID_ATTRIBUTE_KEY
import dev.openfeature.sdk.Client
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Value
import org.springframework.stereotype.Service

@Service
internal class OpenFeatureFeatureFlags(
    private val client: Client,
) : FeatureFlags {

    override fun isEnabled(flag: String): Boolean =
        client.getBooleanValue(flag, false)

    override fun isEnabled(flag: String, context: FeatureFlagContext): Boolean =
        client.getBooleanValue(flag, false, context.toEvaluationContext())
}

// Used by OpenFeatureFeatureFlags for explicit context passed by the caller
internal fun FeatureFlagContext.toEvaluationContext(): ImmutableContext {
    val targetingKey = userId ?: sessionId ?: correlationId ?: ""
    val attrs = buildMap {
        userId?.let { put(USER_ID_ATTRIBUTE_KEY, Value(it)) }
        correlationId?.let { put(CORRELATION_ID_ATTRIBUTE_KEY, Value(it)) }
        email?.let { put(EMAIL_ATTRIBUTE_KEY, Value(it)) }
        put(ADMIN_ATTRIBUTE_KEY, Value(admin))
        attributes.forEach { (key, value) -> put(key, Value(value)) }
    }

    return ImmutableContext(targetingKey, attrs)
}
