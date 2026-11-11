package com.drinkit.ai

import org.springframework.ai.chat.client.ChatClient

/** Each model a test names through [model] is available, and answers what that test scripts. */
class FakeAiModels : AiModels {

    private val models = mutableMapOf<String, FakeChatModel>()

    fun model(model: String): FakeChatModel = models.getOrPut(model) { FakeChatModel() }

    override fun isAvailable(model: String): Boolean = model in models

    override fun chatClient(model: String): ChatClient =
        ChatClient.create(models[model] ?: throw ModelUnavailableException(model))
}
