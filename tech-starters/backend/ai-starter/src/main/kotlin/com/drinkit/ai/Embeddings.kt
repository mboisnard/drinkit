package com.drinkit.ai

import com.drinkit.documentation.tech.starter.TechStarterTool

/**
 * Turns text into vectors with one embedding model of the catalog. A vector compares only with the vectors of the same
 * model: a model change means computing every stored vector again.
 *
 * The [instruction] of a call goes as is before each text, unmasked: the task a model expects in its own format, such
 * as Qwen3's `Instruct: <task>\nQuery: ` before a query. Each use case writes its own.
 */
@TechStarterTool
interface Embeddings {

    val dimensions: Int

    /** Embeds every text in one request, and gives the vectors in the order of the texts. */
    fun embed(texts: List<String>, instruction: String = ""): List<FloatArray>

    fun embed(text: String, instruction: String = ""): FloatArray = embed(listOf(text), instruction).single()
}
