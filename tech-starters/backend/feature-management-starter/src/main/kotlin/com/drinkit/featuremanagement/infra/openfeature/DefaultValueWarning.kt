package com.drinkit.featuremanagement.infra.openfeature

import dev.openfeature.sdk.Hook
import dev.openfeature.sdk.HookContext
import io.github.oshai.kotlinlogging.KotlinLogging

internal class DefaultValueWarning : Hook<Any> {

    private val logger = KotlinLogging.logger { }

    override fun error(ctx: HookContext<Any>, error: Exception, hints: Map<String, Any>) {
        logger.warn { "${error.message}, default value ${ctx.defaultValue} used" }
    }
}
