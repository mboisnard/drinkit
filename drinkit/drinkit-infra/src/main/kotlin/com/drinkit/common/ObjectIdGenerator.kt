package com.drinkit.common

import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
internal class ObjectIdGenerator: IdGenerator {

    override fun createNewId(): String = ObjectId().toHexString()
}