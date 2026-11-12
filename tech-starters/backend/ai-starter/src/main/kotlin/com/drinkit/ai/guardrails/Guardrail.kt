package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse

/**
 * A group of rules checked before the model is called. A bean of the application joins the groups of the starter,
 * set by its [name] under `starters.ai.guardrails.groups`. A bean named after a group of the starter, such as
 * `injectionGuardrail`, replaces it.
 */
@TechStarterTool
interface BeforeCallGuardrail {

    val name: String

    fun check(request: ChatClientRequest): List<Finding>
}

/** A group of rules checked on the answer of the model, set and replaced like a [BeforeCallGuardrail]. */
@TechStarterTool
interface AfterCallGuardrail {

    val name: String

    fun check(request: ChatClientRequest, response: ChatClientResponse): List<Finding>
}
