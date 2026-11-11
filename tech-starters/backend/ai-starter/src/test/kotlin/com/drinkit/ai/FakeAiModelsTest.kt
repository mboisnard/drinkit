package com.drinkit.ai

internal class FakeAiModelsTest : AiModelsTestContract() {

    private val aiModels = FakeAiModels()

    override fun fetchAiModels(): AiModels = aiModels

    override fun givenModel(model: String, answer: String) {
        aiModels.model(model).answers(answer)
    }
}
