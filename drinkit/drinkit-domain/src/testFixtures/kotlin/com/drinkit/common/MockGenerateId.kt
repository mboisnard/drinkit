package com.drinkit.common

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class MockGenerateId: GenerateId {
    val ids = mutableMapOf<KClass<out AbstractId>, Int>()

    override fun <Id : AbstractId> invoke(idType: KClass<Id>): Id {
        val generatedId = ids.compute(idType) { _, value -> if (value == null) 1 else value + 1}!!

        return idType.primaryConstructor!!.call("${idType.simpleName}-$generatedId")
    }
}