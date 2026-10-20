package com.drinkit.architecture

import io.github.baole.konture.Modifier
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

// The starters whose code carries no infrastructure: the domain may depend on them
private val DOMAIN_STARTERS = setOf(
    ":documentation-starter",
    ":event-sourcing-starter",
    ":kotlin-starter",
    ":messaging-starter",
    ":retryable-starter",
    ":test-starter",
)

internal class DomainArchitectureTest {

    @Test
    fun `the domain depends on no infra module`() {
        rule("domain.no-infra-dependency") {
            description = "The domain reaches infra through the ports of its spi package only: remove the dependency " +
                "on the -infra module and call a port"
            modules {
                that().haveNamePath(DOMAINS)
                should().notDependOn(INFRAS)
            }
        }.check()
    }

    @Test
    fun `the domain depends on no backend module`() {
        rule("domain.no-backend-dependency") {
            description = "The backend drives the domain, never the reverse: move what the domain needs from the " +
                "backend into the domain"
            modules {
                that().haveNamePath(DOMAINS)
                should().notDependOn(BACKENDS)
            }
        }.check()
    }

    @Test
    fun `the domain depends only on the starters that carry no infrastructure`() {
        rule("domain.depends-only-on-library-starters") {
            description = "A starter that wires a database, the web or mail is infrastructure: the domain depends " +
                "only on the starters of DOMAIN_STARTERS, which grows by an architecture decision"
            modules {
                that().haveNamePath(DOMAINS)
                should().satisfy { module, violations ->
                    module.dependencies
                        .filter { it.targetPath in STARTERS && it.targetPath !in DOMAIN_STARTERS }
                        .forEach { violations += "${module.path} depends on ${it.targetPath} via ${it.configuration}" }
                }
            }
        }.check()
    }

    @Test
    fun `the domain uses only its own code, its starters, Kotlin, the JDK and Spring's services and transactions`() {
        rule("domain.uses-only-allowed-code") {
            description = "The domain imports only its own code and starters, Kotlin, java.lang, java.time, " +
                "java.math, java.util, KotlinLogging and Spring's @Service, @Component and @Transactional: move any " +
                "other library behind a port"
            val allowed = listOf(
                "kotlin.",
                "java.lang.",
                "java.time.",
                "java.math.",
                "java.util.",
                "io.github.oshai.kotlinlogging.",
                "org.springframework.stereotype.Component",
                "org.springframework.stereotype.Service",
                "org.springframework.transaction.annotation.",
            )
            files {
                that().inModules(DOMAINS)
                should().satisfy { file, violations ->
                    val own = "${rootPackageOf(file.declaration.packageName)}."
                    file.declaration.imports
                        .filterNot { it.startsWith(own) || it.isOfStarter() || allowed.any(it::startsWith) }
                        .forEach { violations += "${file.declaration.name} imports $it" }
                }
            }
        }.check()
    }

    @Test
    fun `the core of a context uses only core and common types`() {
        rule("domain.core-depends-on-core-only") {
            description = "Entities and values never use a use case, a command, a decider or a port: move the " +
                "dependency into the use case, or the type into core"
            classes {
                that().inModule(DOMAINS)
                that().areTopLevel()
                that().satisfy { it.role() == Role.CORE }
                should().satisfy { cls, violations ->
                    cls.referencedDomainClasses()
                        .filter { it.role() == Role.USE_CASES || it.role() == Role.SPI }
                        .forEach { violations += "${cls.fqName} uses ${it.fqName}" }
                }
            }
        }.check()
    }

    @Test
    fun `a port speaks only core and common types`() {
        rule("domain.spi-depends-on-core-only") {
            description = "A port speaks the core and common types, never those of the use cases that call it: move " +
                "the type it needs into core"
            classes {
                that().inModule(DOMAINS)
                that().areTopLevel()
                that().satisfy { it.role() == Role.SPI }
                should().satisfy { port, violations ->
                    port.referencedDomainClasses()
                        .filter { it.role() == Role.USE_CASES }
                        .forEach { violations += "${port.fqName} uses ${it.fqName}" }
                }
            }
        }.check()
    }

    @Test
    fun `a decider uses only its root package, Kotlin, java time and java math, never a port or a clock`() {
        rule("domain.decider-is-pure") {
            description = "A decider decides from its arguments alone: it uses only the types of its application, " +
                "Kotlin, java.time and java.math, never a port or a clock. Pass the values in"
            val clocks = setOf("java.time.Clock", "kotlin.time.Clock")
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("FunctionalCore")
                should().satisfy { cls, violations ->
                    val forbidden = portNames + clocks
                    val allowed = listOf("${rootPackageOf(cls.packageName)}.", "kotlin.", "java.time.", "java.math.")
                    cls.referencedTypeNames()
                        .filter { type -> type in forbidden || (allowed.none(type::startsWith) && !type.isOfStarter()) }
                        .forEach { violations += "${cls.fqName} uses $it" }
                }
            }
        }.check()
    }

    @Test
    fun `the domain reads no system clock and draws no identifier or random value itself`() {
        rule("domain.time-and-ids-from-providers") {
            description = "Tests control time, identifiers and randomness only when they are injected: call " +
                "now(clock) and GenerateId, never Clock.system*(), now(), currentTimeMillis(), UUID.randomUUID() or " +
                "Random"
            suppress {
                file("*/com/drinkit/user/core/ProfileInformation.kt", FOLLOW_UP)
                file("*/com/drinkit/common/CorrelationId.kt", FOLLOW_UP)
                file("*/com/drinkit/user/GenerateVerificationToken.kt", FOLLOW_UP)
            }
            files {
                that().inModules(DOMAINS)
                should().notCall("java.util.UUID.randomUUID")
                andShould().notCall("java.time.Clock.system")
                andShould().notCall("java.time.Clock.systemUTC")
                andShould().notCall("java.time.Clock.systemDefaultZone")
                andShould().notCall("java.lang.System.currentTimeMillis")
                andShould().notCall("java.lang.System.nanoTime")
                andShould().notReferenceClass("kotlin.random.Random")
                andShould().notReferenceClass("java.util.Random")
                andShould().satisfy { file, violations ->
                    file.declaration.usages
                        .filter { it.callsNowWithoutClock() }
                        .forEach { violations += "${file.declaration.name} calls ${it.rawExpression}() with no clock" }
                }
            }
        }.check()
    }

    @Test
    fun `a port is an interface in a spi package`() {
        rule("domain.ports-are-interfaces-in-spi") {
            description = "A port is an interface in the spi package of its context, or of common: move it there, or " +
                "out of spi when it is not a port"
            suppress {
                classFqName("com.drinkit.user.spi.UserCreated", FOLLOW_UP)
                classFqName("com.drinkit.common.GenerateId", FOLLOW_UP)
            }
            classes {
                that().inModule(DOMAINS)
                that().inPackage("..spi..")
                that().areTopLevel()
                should().beInterfaces()
            }
            classes {
                that().inModule(DOMAINS)
                that().satisfy { it in ports }
                should().inPackage("..spi..")
            }
        }.check()
    }

    @Test
    fun `a core domain type lives in a core package`() {
        rule("domain.core-domain-in-core") {
            description = "A @CoreDomain type is an entity or a value: move it into the core package of its context"
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("CoreDomain")
                should().inPackage("..core..")
            }
        }.check()
    }

    @Test
    fun `a use case is a service annotated as a use case`() {
        rule("domain.use-cases-are-annotated") {
            description = "The documentation lists the use cases by their @Usecase: annotate the @Service"
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("Service")
                should().annotatedWith("Usecase")
            }
        }.check()
    }

    @Test
    fun `a command is a data class annotated as a command`() {
        rule("domain.commands-are-annotated-data-classes") {
            description = "A command is the immutable and documented input of a use case: make it a data class " +
                "annotated @Command"
            classes {
                that().inModule(DOMAINS)
                that().nameEndsWith("Command")
                should().beData()
                andShould().annotatedWith("Command")
            }
        }.check()
    }

    @Test
    fun `a decider is an internal object`() {
        rule("domain.decider-is-internal-object") {
            description = "A decider holds no state and only its use case calls it: make it an internal object"
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("FunctionalCore")
                should().beInternal()
                andShould().haveModifier(Modifier.OBJECT)
            }
        }.check()
    }

    @Test
    fun `a domain name says what a thing is, never its pattern`() {
        rule("domain.names-say-what-not-pattern") {
            description = "A domain name says what a thing is, not its pattern, such as Cellars or CreateCellar: " +
                "drop the UseCase, Service, Repository, Impl or Dto suffix"
            classes {
                that().inModule(DOMAINS)
                should().notNameEndsWith("UseCase", "Service", "Repository", "Impl", "Dto", "DTO")
            }
        }.check()
    }
}
