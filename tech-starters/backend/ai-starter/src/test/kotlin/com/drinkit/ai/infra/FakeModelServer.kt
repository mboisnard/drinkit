package com.drinkit.ai.infra

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/** Answers the chat calls of the OpenAI and Anthropic protocols with the answer scripted for the model they name. */
internal class FakeModelServer : AutoCloseable {

    private val json = JsonMapper()
    private val answers = ConcurrentHashMap<String, String>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    val url: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/v1/chat/completions") { exchange -> reply(exchange, ::openAiAnswer) }
        server.createContext("/v1/messages") { exchange -> reply(exchange, ::anthropicAnswer) }
        server.start()
    }

    fun answers(model: String, answer: String) {
        answers[model] = answer
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

        val bytes = json.writeValueAsBytes(body)
        it.responseHeaders.add("Content-Type", "application/json")
        it.sendResponseHeaders(status, bytes.size.toLong())
        it.responseBody.write(bytes)
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
