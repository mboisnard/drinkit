package com.drinkit.ai

import com.drinkit.documentation.tech.starter.TechStarterTool

data class ModelAnswer<T>(val model: String, val value: T)

@TechStarterTool
class InvalidAnswerException(val field: String, cause: Throwable) :
    RuntimeException("The answer of the model is invalid at $field", cause)

@TechStarterTool
class ModelUnavailableException(val model: String) : RuntimeException("Model $model is unavailable")
