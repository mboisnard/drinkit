package com.drinkit.configuration.infra

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations
import com.drinkit.configuration.generated.jooq.tables.references.CONFIGURATION
import com.drinkit.postgresql.JSONBToJacksonConverter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.DSLContext
import org.jooq.exception.DataTypeException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.reflect.javaType

internal const val CONFIGURATIONS_CACHE = "configurations"

// A write belongs to the transaction of the caller's use case, a read may happen outside any
@Transactional(propagation = Propagation.MANDATORY)
internal class JooqConfigurations(
    private val dsl: DSLContext,
    private val jsonMapper: JsonMapper,
    private val clock: Clock,
) : Configurations {

    private val logger = KotlinLogging.logger {}

    @Cacheable(CONFIGURATIONS_CACHE, key = "#key.name")
    @Transactional(readOnly = true)
    override fun <T> get(key: ConfigurationKey<T>): T {
        val stored = dsl.select(CONFIGURATION.VALUE)
            .from(CONFIGURATION)
            .where(CONFIGURATION.KEY.eq(key.name))
            .fetchOne(CONFIGURATION.VALUE)
            ?: return key.default

        return try {
            converterOf(key).from(stored) ?: key.default
        } catch (_: DataTypeException) {
            // The value stays out of the log: it may hold what only the database should see
            logger.error { "The stored value of configuration ${key.name} cannot be read, its default applies" }
            key.default
        }
    }

    @CacheEvict(CONFIGURATIONS_CACHE, key = "#key.name")
    override fun <T> set(key: ConfigurationKey<T>, value: T) {
        val json = converterOf(key).to(value)
        val now = OffsetDateTime.now(clock)

        dsl.insertInto(CONFIGURATION)
            .set(CONFIGURATION.KEY, key.name)
            .set(CONFIGURATION.VALUE, json)
            .set(CONFIGURATION.MODIFIED, now)
            .onConflict(CONFIGURATION.KEY)
            .doUpdate()
            .set(CONFIGURATION.VALUE, json)
            .set(CONFIGURATION.MODIFIED, now)
            .execute()
    }

    @CacheEvict(CONFIGURATIONS_CACHE, key = "#key.name")
    override fun reset(key: ConfigurationKey<*>) {
        dsl.deleteFrom(CONFIGURATION)
            .where(CONFIGURATION.KEY.eq(key.name))
            .execute()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun <T> converterOf(key: ConfigurationKey<T>) =
        JSONBToJacksonConverter<T>(jsonMapper.constructType(key.type.javaType), jsonMapper)
}
