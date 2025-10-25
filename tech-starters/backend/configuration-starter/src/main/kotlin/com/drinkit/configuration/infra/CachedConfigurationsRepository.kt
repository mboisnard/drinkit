package com.drinkit.configuration.infra

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import kotlin.reflect.KClass

@Primary
@Repository
internal class CachedConfigurationsRepository(
    private val delegate: JooqConfigurationsRepository
): Configurations by delegate {

    @Cacheable(value = ["configurations"], key = "#key.key")
    override fun <T : Any> get(key: ConfigurationKey<T>, type: KClass<T>): T? = delegate.get(key, type)

    @CacheEvict(value = ["configurations"], key = "#key.key")
    override fun <T : Any> set(key: ConfigurationKey<T>, value: T, type: KClass<T>): T = delegate.set(key, value, type)

    @CacheEvict(value = ["configurations"], key = "#key.key")
    override fun delete(key: ConfigurationKey<*>) = delegate.delete(key)
}