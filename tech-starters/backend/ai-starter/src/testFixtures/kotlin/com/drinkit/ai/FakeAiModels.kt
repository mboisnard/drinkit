package com.drinkit.ai

import org.springframework.ai.chat.client.ChatClient

/**
 * Each model a test names through [model] or [embeddingModel] is available, and answers or embeds what that test
 * scripts.
 */
class FakeAiModels : AiModels {

    private val models = mutableMapOf<String, FakeChatModel>()
    private val embeddingModels = mutableMapOf<String, FakeEmbeddings>()

    fun model(model: String): FakeChatModel = models.getOrPut(model) { FakeChatModel() }

    fun embeddingModel(model: String, dimensions: Int = 3): FakeEmbeddings =
        embeddingModels.getOrPut(model) { FakeEmbeddings(dimensions) }

    override fun isAvailable(model: String): Boolean = model in models || model in embeddingModels

    override fun chatClient(model: String): ChatClient =
        ChatClient.create(models[model] ?: throw ModelUnavailableException(model))

    override fun embeddings(model: String): Embeddings =
        embeddingModels[model] ?: throw ModelUnavailableException(model)
}
