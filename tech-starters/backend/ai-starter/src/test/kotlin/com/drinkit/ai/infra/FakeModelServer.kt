package com.drinkit.ai.infra

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Answers the chat calls of the OpenAI and Anthropic protocols with the answer scripted for the model they name, and
 * the embedding calls with the vector scripted for each text it knows, skipping the others. Records each embedding
 * request.
 */
internal class FakeModelServer : AutoCloseable {

    private val json = JsonMapper()
    private val answers = ConcurrentHashMap<String, String>()
    private val vectors = ConcurrentHashMap<Pair<String, String>, FloatArray>()
    private val embeddingRequests = mutableListOf<List<String>>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    val url: String get() = "http://127.0.0.1:${server.address.port}"

    /** `false` answers an embedding without its token usage, as LocalAI does. */
    var reportsEmbeddingUsage = true

    /** The texts of each embedding request, in the order they came. */
    val embedded: List<List<String>> get() = synchronized(embeddingRequests) { embeddingRequests.toList() }

    init {
        server.createContext("/v1/chat/completions") { exchange -> reply(exchange, ::openAiAnswer) }
        server.createContext("/v1/messages") { exchange -> reply(exchange, ::anthropicAnswer) }
        server.createContext("/v1/embeddings", ::embed)
        server.start()
    }

    fun answers(model: String, answer: String) {
        answers[model] = answer
    }

    fun embeds(model: String, text: String, vector: FloatArray) {
        vectors[model to text] = vector
    }

    override fun close() = server.stop(0)

    private fun reply(exchange: HttpExchange, answer: (String, String) -> Map<String, Any?>) = exchange.use {
        val model = json.readTree(it.requestBody.readAllBytes()).get("model").asString()
        val text = answers[model]
        val (status, body) = if (text == null) {
            NOT_FOUND to mapOf("error" to mapOf("message" to "No model $model"))
        } else {
            OK to answer(model, text)
        }
        send(it, status, body)
    }

    private fun embed(exchange: HttpExchange) = exchange.use {
        val request = json.readTree(it.requestBody.readAllBytes())
        val model = request.get("model").asString()
        val texts = buildList { request.get("input").forEach { input -> add(input.asString()) } }
        synchronized(embeddingRequests) { embeddingRequests += texts }

        val data = texts.mapNotNull { text -> vectors[model to text] }.mapIndexed { index, vector ->
            mapOf("object" to "embedding", "index" to index, "embedding" to vector.toList())
        }
        val usage = mapOf("prompt_tokens" to PROMPT_TOKENS, "total_tokens" to PROMPT_TOKENS)
        val body = mapOf("object" to "list", "data" to data, "model" to model)
        send(it, OK, if (reportsEmbeddingUsage) body + ("usage" to usage) else body)
    }

    private fun send(exchange: HttpExchange, status: Int, body: Map<String, Any?>) {
        val bytes = json.writeValueAsBytes(body)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun openAiAnswer(model: String, text: String) = mapOf(
        "id" to "chatcmpl-1",
        "object" to "chat.completion",
        "created" to 1,
        "model" to model,
        "choices" to listOf(
            mapOf(
                "index" to 0,
                "message" to mapOf("role" to "assistant", "content" to text),
                "finish_reason" to "stop",
            ),
        ),
        "usage" to mapOf("prompt_tokens" to PROMPT_TOKENS, "completion_tokens" to ANSWER_TOKENS, "total_tokens" to 15),
    )

    private fun anthropicAnswer(model: String, text: String) = mapOf(
        "id" to "msg_1",
        "type" to "message",
        "role" to "assistant",
        "model" to model,
        "content" to listOf(mapOf("type" to "text", "text" to text)),
        "stop_reason" to "end_turn",
        "stop_sequence" to null,
        "usage" to mapOf("input_tokens" to PROMPT_TOKENS, "output_tokens" to ANSWER_TOKENS),
    )

    companion object {
        const val PROMPT_TOKENS = 12
        const val ANSWER_TOKENS = 3
        private const val OK = 200
        private const val NOT_FOUND = 404
    }
}
