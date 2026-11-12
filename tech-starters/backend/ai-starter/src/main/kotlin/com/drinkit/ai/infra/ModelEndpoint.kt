package com.drinkit.ai.infra

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel

/** Holds the SDK clients of one endpoint, which every model of that endpoint shares. */
internal interface ModelEndpoint {

    fun chatModel(model: String): ChatModel

    fun embeddingModel(model: String): EmbeddingModel
}
