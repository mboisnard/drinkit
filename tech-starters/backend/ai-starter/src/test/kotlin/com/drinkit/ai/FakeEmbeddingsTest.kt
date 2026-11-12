package com.drinkit.ai

internal class FakeEmbeddingsTest : EmbeddingsTestContract() {

    private val embeddings = FakeEmbeddings()

    override fun fetchEmbeddings(): Embeddings = embeddings

    override fun givenVector(text: String, vector: FloatArray) {
        embeddings.embeds(text, vector)
    }

    override fun receivedTexts(): List<String> = embeddings.texts
}
