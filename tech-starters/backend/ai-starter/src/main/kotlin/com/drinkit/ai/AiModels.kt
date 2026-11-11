package com.drinkit.ai

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.ai.chat.client.ChatClient

/** Names a model by its id in the catalog of `starters.ai.models`, such as `qwen3-vl-4b`. */
@TechStarterTool
interface AiModels {

    fun isAvailable(model: String): Boolean

    /** @throws ModelUnavailableException when the model is unknown, its key is missing or the starter is off */
    fun chatClient(model: String): ChatClient
}

/** The message names the models only: the errors of the providers stay in [causes], out of any response. */
@TechStarterTool
class AllModelsFailedException(val causes: Map<String, Throwable>) :
    RuntimeException("No model answered among ${causes.keys.joinToString()}")
