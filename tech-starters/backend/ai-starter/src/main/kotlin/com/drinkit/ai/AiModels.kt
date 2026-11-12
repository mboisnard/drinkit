package com.drinkit.ai

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.ai.chat.client.ChatClient

/** Names a model by its id in the catalog of `starters.ai.models`, such as `qwen3-vl-4b`. */
@TechStarterTool
interface AiModels {

    fun isAvailable(model: String): Boolean

    /** @throws ModelUnavailableException when the model is unknown, its key is missing or the starter is off */
    fun chatClient(model: String): ChatClient

    /**
     * @throws ModelUnavailableException when the model is unknown, is no embedding model, its key is missing or the
     * starter is off
     */
    fun embeddings(model: String): Embeddings
}

@TechStarterTool
class ModelUnavailableException(val model: String) : RuntimeException("Model $model is unavailable")
