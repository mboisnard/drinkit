package com.drinkit.ai

/** Gives the vector a test scripts for each text as the model receives it, after its instruction, and records them. */
class FakeEmbeddings(override val dimensions: Int = 3) : Embeddings {

    private val vectors = mutableMapOf<String, FloatArray>()
    private val received = mutableListOf<String>()

    val texts: List<String> get() = received.toList()

    fun embeds(text: String, vector: FloatArray): FakeEmbeddings = apply { vectors[text] = vector }

    override fun embed(texts: List<String>, instruction: String): List<FloatArray> =
        texts.map { text -> vectorOf(instruction + text) }

    private fun vectorOf(text: String): FloatArray {
        received += text

        val vector = vectors[text] ?: error("No vector scripted for \"$text\"")
        check(vector.size == dimensions) {
            "The model returned a vector of ${vector.size} dimensions, its catalog declares $dimensions"
        }

        return vector
    }
}
