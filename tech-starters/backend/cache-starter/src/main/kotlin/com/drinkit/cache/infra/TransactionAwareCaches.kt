package com.drinkit.cache.infra

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.cache.CacheManager
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy

// Holds each put and eviction back until the transaction commits: a rollback leaves the cache as it was
internal class TransactionAwareCaches : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any =
        if (bean is CacheManager && bean !is TransactionAwareCacheManagerProxy) {
            TransactionAwareCacheManagerProxy(bean)
        } else {
            bean
        }
}
