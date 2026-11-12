package com.drinkit.ai.infra

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

/**
 * Adds an empty token usage to an embedding answer that has none, as LocalAI's: the OpenAI protocol requires it, and
 * the OpenAI SDK fails on every answer without it.
 */
internal class MissingEmbeddingUsage : Interceptor {

    private val json = JsonMapper()

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body
        val isEmbedding = response.isSuccessful && chain.request().url.encodedPath.endsWith("/embeddings")
        if (!isEmbedding || body == null) return response

        val answer = withUsage(body.string())

        return response.newBuilder().body(answer.toResponseBody(body.contentType())).build()
    }

    /** Leaves an answer that is no JSON object as it is, for the SDK to report. */
    private fun withUsage(answer: String): String {
        val tree = runCatching { json.readTree(answer) }.getOrNull()
        if (tree !is ObjectNode || tree.has("usage")) return answer

        tree.putObject("usage").put("prompt_tokens", 0).put("total_tokens", 0)

        return json.writeValueAsString(tree)
    }
}
