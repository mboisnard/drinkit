package com.drinkit.postgresql.jooq

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.JSONB
import org.jooq.exception.DataTypeException
import org.jooq.impl.AbstractConverter

/**
 * Why not just use JSONBtoJacksonConverter provided by jooq-jackson-extension?
 * Just to be able to control the objectMapper used for serialization/deserialization
 * and be able to include project modules
 */
class JSONBToJacksonConverter<U>(
    toType: Class<U>,
    private val mapper: ObjectMapper,
) : AbstractConverter<JSONB, U>(JSONB::class.java, toType) {

    override fun from(databaseObject: JSONB): U {
        try {
            return mapper.readValue<U>(databaseObject.data(), toType())
        } catch (e: JsonProcessingException) {
            throw DataTypeException("Error when converting JSON to ${toType()}", e)
        }
    }

    override fun to(userObject: U): JSONB {
        try {
            return JSONB.jsonb(mapper.writeValueAsString(userObject))
        } catch (e: JsonProcessingException) {
            throw DataTypeException("Error when converting object of type ${toType()} to JSON", e)
        }
    }
}
