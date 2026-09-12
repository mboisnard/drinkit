package com.drinkit.postgresql.jooq

import com.drinkit.documentation.tech.starter.TechStarterTool
import tools.jackson.core.JacksonException
import org.jooq.JSONB
import org.jooq.exception.DataTypeException
import org.jooq.impl.AbstractConverter
import tools.jackson.databind.json.JsonMapper

/**
 * Why not just use JSONBtoJacksonConverter provided by jooq-jackson-extension?
 * Just to be able to control the jsonMapper used for serialization/deserialization
 * and be able to include project modules
 */
@TechStarterTool
class JSONBToJacksonConverter<U>(
    toType: Class<U>,
    private val mapper: JsonMapper,
) : AbstractConverter<JSONB, U>(JSONB::class.java, toType) {

    override fun from(databaseObject: JSONB): U {
        try {
            return mapper.readValue<U>(databaseObject.data(), toType())
        } catch (e: JacksonException) {
            throw DataTypeException("Error when converting JSON to ${toType()}", e)
        }
    }

    override fun to(userObject: U): JSONB {
        try {
            return JSONB.jsonb(mapper.writeValueAsString(userObject))
        } catch (e: JacksonException) {
            throw DataTypeException("Error when converting object of type ${toType()} to JSON", e)
        }
    }
}
