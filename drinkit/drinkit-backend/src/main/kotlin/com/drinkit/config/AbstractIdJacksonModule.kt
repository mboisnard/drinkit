package com.drinkit.config

import com.drinkit.cellar.CellarId
import com.drinkit.common.AbstractId
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule

class AbstractIdSerializer : JsonSerializer<AbstractId>() {

    override fun serialize(id: AbstractId, generator: JsonGenerator, serializer: SerializerProvider) {
        generator.writeString(id.value)
    }
}

class AbstractIdJacksonModule : SimpleModule() {

    init {
        addSerializer(CellarId::class.java, AbstractIdSerializer())
    }
}
