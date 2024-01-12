package com.drinkit.config

import com.drinkit.common.AbstractId
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleSerializers

class AbstractIdSerializer : JsonSerializer<AbstractId>() {

    override fun serialize(id: AbstractId, generator: JsonGenerator, serializer: SerializerProvider) {
        generator.writeString(id.value)
    }
}

class AbstractIdJacksonModule : SimpleModule() {

    override fun setupModule(context: SetupContext) {
        val serializers = SimpleSerializers()
        serializers.addSerializer(AbstractId::class.java, AbstractIdSerializer())
        context.addSerializers(serializers)

        super.setupModule(context)
    }
}