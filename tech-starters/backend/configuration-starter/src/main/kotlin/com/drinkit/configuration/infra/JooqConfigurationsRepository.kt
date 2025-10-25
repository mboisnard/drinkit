package com.drinkit.configuration.infra

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations
import com.drinkit.configuration.generated.jooq.tables.references.CONFIGURATION
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.reflect.KClass

@Repository
internal class JooqConfigurationsRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : Configurations {

    @Transactional(readOnly = true)
    override fun <T : Any> get(key: ConfigurationKey<T>, type: KClass<T>): T? {
         val record = dsl.selectFrom(CONFIGURATION)
             .where(CONFIGURATION.KEY.eq(key.key))
             .fetchOne()
             ?: return null

         return objectMapper.readValue(record.value.data(), type.java)
    }

    @Transactional
    override fun <T : Any> set(key: ConfigurationKey<T>, value: T, type: KClass<T>): T {
        val date = OffsetDateTime.now(clock)
        val serializedValue = JSONB.jsonb(objectMapper.writeValueAsString(value))

        dsl.insertInto(CONFIGURATION)
             .set(CONFIGURATION.KEY, key.key)
             .set(CONFIGURATION.VALUE, serializedValue)
             .set(CONFIGURATION.MODIFIED, date)
             .onConflict(CONFIGURATION.KEY)
             .doUpdate()
             .set(CONFIGURATION.VALUE, serializedValue)
             .set(CONFIGURATION.MODIFIED, date)
             .execute()

         return value
    }

    @Transactional
    override fun delete(key: ConfigurationKey<*>) {
         dsl.deleteFrom(CONFIGURATION)
             .where(CONFIGURATION.KEY.eq(key.key))
             .execute()
    }
}
