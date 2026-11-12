package com.drinkit.ai

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

/** Answers its scripted replies in order, and records every prompt it receives. */
class FakeChatModel : ChatModel {

    private val replies = ArrayDeque<() -> ChatResponse>()
    private val received = mutableListOf<Prompt>()

    val prompts: List<Prompt> get() = received.toList()

    fun answers(vararg answers: String): FakeChatModel = apply {
        answers.forEach { answer -> replies.addLast { ChatResponse(listOf(Generation(AssistantMessage(answer)))) } }
    }

    /** Answers [answer], reporting that the call used [tokens] tokens. */
    fun answers(answer: String, tokens: Int): FakeChatModel = apply {
        val usage = ChatResponseMetadata.builder().usage(DefaultUsage(0, tokens, tokens)).build()
        replies.addLast { ChatResponse(listOf(Generation(AssistantMessage(answer))), usage) }
    }

    fun fails(cause: RuntimeException): FakeChatModel = apply {
        replies.addLast { throw cause }
    }

    override fun call(prompt: Prompt): ChatResponse {
        received += prompt

        val reply = replies.removeFirstOrNull() ?: error("No answer scripted for this prompt")
        return reply()
    }
}
