package com.drinkit.config

import com.drinkit.common.AbstractId
import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

class AbstractIdSerializer : ValueSerializer<AbstractId>() {

    override fun serialize(id: AbstractId, generator: JsonGenerator, serializer: SerializationContext) {
        generator.writeString(id.value)
    }
}

abstract class AbstractIdMixin(
    @field:JsonValue @get:JsonValue val value: String,
)

class AbstractIdJacksonModule : SimpleModule() {

    init {
        addSerializer(AbstractId::class.java, AbstractIdSerializer())
        setMixInAnnotation(AbstractId::class.java, AbstractIdMixin::class.java)
    }
}
