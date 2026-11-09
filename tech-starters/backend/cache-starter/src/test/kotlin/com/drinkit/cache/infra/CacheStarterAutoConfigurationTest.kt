package com.drinkit.cache.infra

import com.drinkit.cache.CacheTtl
import com.drinkit.cache.ControlledTicker
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Ticker
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

internal class CacheStarterAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(CacheStarterAutoConfiguration::class.java, CacheAutoConfiguration::class.java),
        )

    @Test
    fun `a cacheable method runs once per key while its entry lives`() {
        runner.withBean(Library::class.java)
            .run { context ->
                // Given
                val library = context.getBean<Library>()

                // When
                repeat(3) { library.findTitle("dune") }

                // Then
                library.readings shouldBe 1
            }
    }

    @Test
    fun `a cacheable method runs again once the time to live of its entry has passed`() {
        val ticker = ControlledTicker()
        runner.withBean(Library::class.java)
            .withBean(Ticker::class.java, { ticker })
            .withBean(CacheTtl::class.java, { CacheTtl("books", Duration.ofSeconds(30)) })
            .run { context ->
                // Given
                val library = context.getBean<Library>()
                library.findTitle("dune")
                ticker.advance(Duration.ofSeconds(29))
                library.findTitle("dune")

                // When
                ticker.advance(Duration.ofSeconds(2))
                library.findTitle("dune")

                // Then
                library.readings shouldBe 2
            }
    }

    @Test
    fun `caches nothing that a transaction read before it rolled back`() {
        runner.withBean(Library::class.java)
            .run { context ->
                // Given
                val library = context.getBean<Library>()

                // When: a transaction that ends without a commit
                TransactionSynchronizationManager.initSynchronization()
                try {
                    library.findTitle("dune")
                } finally {
                    TransactionSynchronizationManager.clearSynchronization()
                }
                library.findTitle("dune")

                // Then
                library.readings shouldBe 2
            }
    }

    @Test
    fun `a cache without a time to live of its own takes the default one`() {
        runner.run { context ->
            // When & Then
            context.ttlOf("books") shouldBe Duration.ofMinutes(10)
        }
    }

    @Test
    fun `a cache takes the time to live its module declares`() {
        runner.withBean(CacheTtl::class.java, { CacheTtl("books", Duration.ofSeconds(30)) })
            .run { context ->
                // When & Then
                context.ttlOf("books") shouldBe Duration.ofSeconds(30)
            }
    }

    @Test
    fun `the application overrides the time to live its module declares`() {
        runner.withBean(CacheTtl::class.java, { CacheTtl("books", Duration.ofSeconds(30)) })
            .withPropertyValues("starters.cache.ttl.books=5s")
            .run { context ->
                // When & Then
                context.ttlOf("books") shouldBe Duration.ofSeconds(5)
            }
    }

    @Test
    fun `the application changes the default time to live`() {
        runner.withPropertyValues("starters.cache.default-ttl=1h")
            .run { context ->
                // When & Then
                context.ttlOf("books") shouldBe Duration.ofHours(1)
            }
    }

    @Test
    fun `each cache holds a bounded number of entries`() {
        runner.run { context ->
            // When & Then
            context.caffeineCache("books").policy().eviction().get().maximum shouldBe 10_000
        }
    }

    @Test
    fun `each cache records its hits and misses`() {
        runner.run { context ->
            // When & Then
            context.caffeineCache("books").policy().isRecordingStats shouldBe true
        }
    }

    @Test
    fun `the starter backs off when the application declares a bean named cacheStarterCaffeineCustomizer`() {
        runner.withBean(
            "cacheStarterCaffeineCustomizer",
            CacheManagerCustomizer::class.java,
            { CacheManagerCustomizer<CaffeineCacheManager> { } },
        )
            .run { context ->
                // When & Then
                context.ttlOf("books") shouldBe null
            }
    }

    @Test
    fun `no cache manager exists when the starter is switched off`() {
        runner.withPropertyValues("starters.cache.enabled=false")
            .run { context ->
                // When & Then
                context.getBeanNamesForType(CacheManager::class.java).toList().shouldBeEmpty()
            }
    }

    private fun AssertableApplicationContext.ttlOf(name: String): Duration? =
        caffeineCache(name).policy().expireAfterWrite().map { it.expiresAfter }.orElse(null)

    private fun AssertableApplicationContext.caffeineCache(name: String): Cache<Any, Any> {
        @Suppress("UNCHECKED_CAST")
        return getBean<CacheManager>().getCache(name)!!.nativeCache as Cache<Any, Any>
    }

    open class Library {
        open var readings = 0

        @Cacheable("books")
        open fun findTitle(id: String): String {
            readings++
            return "Title of $id"
        }
    }
}
