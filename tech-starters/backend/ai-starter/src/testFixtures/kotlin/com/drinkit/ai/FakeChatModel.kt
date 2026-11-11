package com.drinkit.ai

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

/** Answers its scripted replies in order, and records every prompt it receives. */
class FakeChatModel : ChatModel {

    private val replies = ArrayDeque<() -> String>()
    private val received = mutableListOf<Prompt>()

    val prompts: List<Prompt> get() = received.toList()

    fun answers(vararg answers: String): FakeChatModel = apply {
        answers.forEach { answer -> replies.addLast { answer } }
    }

    fun fails(cause: RuntimeException): FakeChatModel = apply {
        replies.addLast { throw cause }
    }

    override fun call(prompt: Prompt): ChatResponse {
        received += prompt

        val reply = replies.removeFirstOrNull() ?: error("No answer scripted for this prompt")
        return ChatResponse(listOf(Generation(AssistantMessage(reply()))))
    }
}
