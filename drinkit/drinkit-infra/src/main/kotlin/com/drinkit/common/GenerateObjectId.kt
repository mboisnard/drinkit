package com.drinkit.common

import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

@Component
internal class GenerateObjectId : GenerateId {
    override fun <Id : AbstractId> invoke(idType: KClass<Id>): Id =
        idType.primaryConstructor!!.call(ObjectId().toHexString())
}