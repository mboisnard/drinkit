package com.drinkit.postgresql

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.jooq.JSONB
import org.jooq.exception.DataTypeException
import org.jooq.impl.AbstractConverter
import tools.jackson.core.JacksonException
import tools.jackson.databind.JavaType
import tools.jackson.databind.json.JsonMapper

/**
 * Why not just use JSONBtoJacksonConverter provided by jooq-jackson-extension?
 * Just to be able to control the jsonMapper used for serialization/deserialization
 * and be able to include project modules
 */
@TechStarterTool
class JSONBToJacksonConverter<U>(private val type: JavaType, private val mapper: JsonMapper) :
    AbstractConverter<JSONB, U>(JSONB::class.java, type.rawClassOf()) {

    // A Class loses the type of the elements of a collection: a JavaType built by the type factory keeps it
    constructor(toType: Class<U>, mapper: JsonMapper) : this(mapper.constructType(toType), mapper)

    override fun from(databaseObject: JSONB): U {
        try {
            return mapper.readValue<U>(databaseObject.data(), type)
        } catch (e: JacksonException) {
            throw DataTypeException("Error when converting JSON to $type", e)
        }
    }

    override fun to(userObject: U): JSONB {
        try {
            return JSONB.jsonb(mapper.writeValueAsString(userObject).replace(NULL_CHARACTER, "$1"))
        } catch (e: JacksonException) {
            throw DataTypeException("Error when converting object of type $type to JSON", e)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <U> JavaType.rawClassOf(): Class<U> = rawClass as Class<U>

// PostgreSQL refuses \u0000 in jsonb: drop the escape, not a backslash that escapes the backslash before it
private val NULL_CHARACTER = Regex("""(?<!\\)((?:\\\\)*)\\u0000""")
