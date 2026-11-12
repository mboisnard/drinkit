package com.drinkit.ai.infra.guardrails

import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

internal fun requestWith(user: String = "Which region is Pommard in?", system: String? = null): ChatClientRequest {
    val messages = listOfNotNull(system?.let(::SystemMessage), UserMessage(user))
    return ChatClientRequest.builder().prompt(Prompt(messages)).build()
}

internal fun responseWith(
    answer: String,
    finishReason: String? = null,
    properties: Map<String, Any> = emptyMap(),
): ChatClientResponse {
    val message = AssistantMessage.builder().content(answer).properties(properties).build()
    val metadata = ChatGenerationMetadata.builder().finishReason(finishReason).build()
    val chatResponse = ChatResponse(listOf(Generation(message, metadata)))
    return ChatClientResponse.builder().chatResponse(chatResponse).build()
}
