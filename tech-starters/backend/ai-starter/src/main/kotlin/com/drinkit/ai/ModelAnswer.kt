package com.drinkit.ai

import com.drinkit.ai.guardrails.Finding
import com.drinkit.documentation.tech.starter.TechStarterTool

/** The answer of [model], and what the guardrails found on its call without blocking it. */
@TechStarterTool
data class ModelAnswer<T>(val model: String, val value: T, val findings: List<Finding> = emptyList())

@TechStarterTool
class InvalidAnswerException(val field: String, cause: Throwable) :
    RuntimeException("The answer of the model is invalid at $field", cause)

@TechStarterTool
class ModelUnavailableException(val model: String) : RuntimeException("Model $model is unavailable")
