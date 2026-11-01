package com.drinkit.architecture

import io.github.baole.konture.UsageKind
import io.github.baole.konture.Visibility
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

// Spring runs these annotations through a proxy, which a call from the same object skips
private val PROXIED = listOf(
    "Transactional",
    "RetryableTransactional",
    "Async",
    "Cacheable",
    "CachePut",
    "CacheEvict",
    "Retryable",
)

internal class SpringArchitectureTest {

    @Test
    fun `settings are never read one by one`() {
        rule("spring.no-value-or-environment-lookup") {
            description = "A setting read with @Value or from the Environment escapes validation and documentation: " +
                "bind it to a @ConfigurationProperties class"
            suppress {
                file("*/com/drinkit/ocr/analyzers/OCRAnalyzer.kt", OPS_STARTERS)
                file("*/com/drinkit/ocr/OCRConfiguration.kt", OPS_STARTERS)
            }
            files {
                that().inModules(ALL)
                should().notReferenceClass("org.springframework.beans.factory.annotation.Value")
                andShould().notReferenceClass("org.springframework.core.env.Environment")
            }
        }.check()
    }

    @Test
    fun `a class never calls its own proxied method`() {
        rule("spring.no-self-invocation-of-proxied-method") {
            description = "A call on this skips the proxy, so @Transactional, @Cacheable, @Async or a retry never " +
                "applies: call the method from another bean"
            classes {
                that().inModule(ALL)
                should().satisfy { cls, violations ->
                    val classProxied = cls.annotations.any { it.name in PROXIED }
                    val proxied = cls.functions
                        .filter { function ->
                            function.annotations.any { it.name in PROXIED } ||
                                (classProxied && function.visibility != Visibility.PRIVATE)
                        }
                        .flatMap { listOf(it.name, "this.${it.name}") }
                        .toSet()
                    cls.usages()
                        .filter { it.kind == UsageKind.CALL && it.rawExpression in proxied }
                        .forEach { violations += "${cls.fqName}.${it.enclosingFunction} calls ${it.rawExpression}" }
                }
            }
        }.check()
    }

    @Test
    fun `beans are injected through their constructor`() {
        rule("spring.constructor-injection-only") {
            description = "A bean is complete once built and testable without Spring: replace the @Autowired, " +
                "@Inject or @Resource field with a constructor parameter"
            classes {
                that().inModule(ALL)
                should().notReferenceClass("org.springframework.beans.factory.annotation.Autowired")
                andShould().notReferenceClass("jakarta.inject.Inject")
                andShould().notReferenceClass("jakarta.annotation.Resource")
            }
        }.check()
    }

    @Test
    fun `a proxied method is visible to its proxy`() {
        rule("spring.proxied-method-is-visible") {
            description = "A proxy intercepts only the methods it can override: make the annotated method public or " +
                "internal"
            functions {
                that().resideInModules(ALL)
                that().haveAnyAnnotationOf(PROXIED)
                should().haveAnyVisibility(Visibility.PUBLIC, Visibility.INTERNAL)
            }
        }.check()
    }

    @Test
    fun `a settings class is internal and its name ends in Properties`() {
        rule("spring.settings-classes-internal-and-named") {
            description = "A @ConfigurationProperties class belongs to its module and says what it binds: make it " +
                "internal and name it *Properties"
            classes {
                that().inModule(ALL)
                that().annotatedWith("ConfigurationProperties")
                should().beInternal()
                andShould().nameEndsWith("Properties")
            }
        }.check()
    }

    @Test
    fun `a configuration class of an application is internal`() {
        rule("spring.configuration-classes-internal") {
            description = "The @Configuration of an application wires its own module only: make it internal. A tech " +
                "starter's follows starters.public-api-is-marked"
            classes {
                that().inModule(APPLICATIONS)
                that().annotatedWith("Configuration")
                should().beInternal()
            }
        }.check()
    }

    @Test
    fun `a configuration class is named after its configuration`() {
        rule("spring.configuration-named-configuration") {
            description = "The wiring of a module is found by its name: rename the @Configuration class " +
                "*Configuration"
            suppress {
                classFqName("com.drinkit.configuration.infra.ConfigurationSpringConfig", DATA_STARTERS)
                classFqName("com.drinkit.postgresql.jooq.JooqConfig", DATA_STARTERS)
                classFqName("com.drinkit.search.engine.MeilisearchConfig", DATA_STARTERS)
                classFqName("com.drinkit.security.SecurityConfig", WEB_STARTERS)
                classFqName("com.drinkit.featureflags.infra.FeatureFlagsConfig", OPS_STARTERS)
            }
            classes {
                that().inModule(ALL)
                that().annotatedWith("Configuration")
                should().nameEndsWith("Configuration")
            }
        }.check()
    }
}
