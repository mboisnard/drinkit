package com.drinkit.common

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class FakeGenerateId : GenerateId {
    val ids = mutableMapOf<KClass<out AbstractId>, Int>()

    override fun <Id : AbstractId> invoke(idType: KClass<Id>): Id {
        val generatedId = ids.getOrDefault(idType, 0) + 1
        ids[idType] = generatedId

        return checkNotNull(idType.primaryConstructor).call("${idType.simpleName}-$generatedId")
    }
}
