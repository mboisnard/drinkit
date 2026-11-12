package com.drinkit.ai.infra.guardrails.after

import org.springframework.ai.chat.client.ChatClientResponse

internal fun answerOf(response: ChatClientResponse): String =
    response.chatResponse()?.results.orEmpty().joinToString("\n") { it.output.text.orEmpty() }
