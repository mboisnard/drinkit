package com.drinkit.featuremanagement.infra.openfeature

import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.featuremanagement.FlagSubject
import dev.openfeature.sdk.Client
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Value

internal class OpenFeatureFeatureFlags(private val client: Client, private val currentSubject: () -> FlagSubject) :
    FeatureFlags {

    override fun isEnabled(flag: String, default: Boolean) = isEnabled(flag, currentSubject(), default)

    override fun isEnabled(flag: String, subject: FlagSubject, default: Boolean): Boolean =
        client.getBooleanValue(flag, default, subject.toContext())
}

internal fun FlagSubject.toContext(): EvaluationContext =
    ImmutableContext(key, attributes.mapValues { (_, value) -> Value(value) })
