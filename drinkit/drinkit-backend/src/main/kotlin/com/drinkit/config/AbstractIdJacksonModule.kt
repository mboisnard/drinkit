package com.drinkit.config

import com.drinkit.common.AbstractId
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule

class AbstractIdSerializer : JsonSerializer<AbstractId>() {

    override fun serialize(id: AbstractId, generator: JsonGenerator, serializer: SerializerProvider) {
        generator.writeString(id.value)
    }
}

abstract class AbstractIdMixin(@field:JsonValue @get:JsonValue val value: String)

class AbstractIdJacksonModule : SimpleModule() {

    init {
        addSerializer(AbstractId::class.java, AbstractIdSerializer())
        setMixInAnnotation(AbstractId::class.java, AbstractIdMixin::class.java)
    }
}
