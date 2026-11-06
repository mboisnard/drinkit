package com.drinkit.security.infra.session

import com.drinkit.security.infra.events.SecurityEventLogger
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.security.jackson.SecurityJacksonModules
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

/**
 * Reads back only texts and the types Spring Security allows in its Jackson modules, so that a forged session row
 * cannot build an arbitrary class. An unreadable attribute is absent: the user is signed out, not refused.
 *
 * OWASP: ASVS 5.0 V1.5.2, Deserialization Cheat Sheet (allowlist of types).
 */
internal class JsonSessionConverter(classLoader: ClassLoader, private val events: SecurityEventLogger) :
    GenericConversionService() {

    private val jsonMapper = JsonMapper.builder()
        .addModules(SecurityJacksonModules.getModules(classLoader, BasicPolymorphicTypeValidator.builder()))
        .build()

    init {
        addConverter(Any::class.java, ByteArray::class.java) { jsonMapper.writeValueAsBytes(it) }
        // A GenericConverter, since a plain Converter may not return null, which an unreadable attribute becomes
        addConverter(
            object : GenericConverter {
                override fun getConvertibleTypes() = setOf(ConvertiblePair(ByteArray::class.java, Any::class.java))

                override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor) =
                    read(source as ByteArray)
            },
        )
    }

    private fun read(json: ByteArray): Any? = try {
        jsonMapper.readValue(json, Any::class.java)
    } catch (_: JacksonException) {
        events.sessionDeserializationFailed()
        null
    }
}
