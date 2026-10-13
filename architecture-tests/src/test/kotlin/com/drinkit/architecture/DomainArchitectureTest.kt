package com.drinkit.architecture

import io.github.baole.konture.Modifier
import io.github.baole.konture.SourceUsage
import io.github.baole.konture.UsageKind
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test
import java.io.File

internal class DomainArchitectureTest {

    @Test
    fun `the domain reaches infrastructure only through its ports`() {
        rule("domain.no-infrastructure-dependency") {
            description = "The domain reaches databases, JSON, HTTP, the web, the Jakarta APIs and the infra modules " +
                "only through a port of its spi package: move this code into an adapter of the infra module"
            modules {
                that().haveNamePath(DOMAINS)
                should().notDependOn(INFRAS)
            }
            files {
                that().inModules(DOMAINS)
                should().notDependOnPackages(
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "jakarta..",
                    "java.sql..",
                    "javax.sql..",
                )
            }
        }.check()
    }

    @Test
    fun `the domain depends on no backend module`() {
        rule("domain.no-backend-dependency") {
            description = "The backend drives the domain through its use cases, never the other way round: move what " +
                "the domain needs from the backend into the domain"
            modules {
                that().haveNamePath(DOMAINS)
                should().notDependOn(BACKENDS)
            }
        }.check()
    }

    @Test
    fun `the domain uses Spring only for services, components and transactions`() {
        rule("domain.spring-only-for-services-components-and-transactions") {
            description = "The domain stays free of Spring but for @Service, @Component and @Transactional, or a " +
                "starter annotation built on them such as @RetryableTransactional: move this code into the infra or " +
                "the backend module"
            val allowed = listOf(
                "org.springframework.stereotype.Component",
                "org.springframework.stereotype.Service",
                "org.springframework.transaction.annotation.",
            )
            files {
                that().inModules(DOMAINS)
                should().satisfy { file, violations ->
                    file.declaration.namesUsed()
                        .filter { it.startsWith("org.springframework.") && allowed.none(it::startsWith) }
                        .forEach { violations += "${file.declaration.name} uses $it" }
                }
            }
        }.check()
    }

    @Test
    fun `a port is an interface in a spi package`() {
        rule("domain.ports-are-interfaces-in-spi") {
            description = "A port is an interface in the spi package of its context, or of common, which the " +
                "adapters implement: move this type there, or out of spi when it is not a port"
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
            description = "A type annotated @CoreDomain is an entity or a value of its context: move it into the " +
                "core package of that context"
            suppress { classFqName("com.drinkit.money.Money", FOLLOW_UP) }
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("CoreDomain")
                should().inPackage("..core..")
            }
        }.check()
    }

    @Test
    fun `the core of a context never uses its use cases, commands or deciders`() {
        rule("domain.core-ignores-its-context-package") {
            description = "Entities and values know nothing of the use cases that drive them, nor of what sits " +
                "beside the use cases in the package of the context, commands and deciders: move the dependency " +
                "into the use case"
            classes {
                that().inModule(DOMAINS)
                that().areTopLevel()
                that().inPackage("..core..")
                should().satisfy { cls, violations ->
                    val useCasesPackage = cls.packageName.substringBefore(".core")
                    cls.referencedTypeNames()
                        .filter { it.substringBeforeLast('.') == useCasesPackage }
                        .forEach { violations += "${cls.fqName} uses $it" }
                }
            }
        }.check()
    }

    @Test
    fun `a use case is a service annotated as a use case`() {
        rule("domain.use-cases-are-annotated") {
            description = "A use case is a @Service annotated @Usecase, which documents it: add the annotation"
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
            description = "A command is the immutable input of a use case, documented as such: make it a data " +
                "class annotated @Command"
            suppress { classFqName("com.drinkit.cellar.CreateCellarCommand", FOLLOW_UP) }
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
            description = "A decider holds no state and is called by its use case only: make it an internal " +
                "object"
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("FunctionalCore")
                should().beInternal()
                andShould().haveModifier(Modifier.OBJECT)
            }
        }.check()
    }

    @Test
    fun `a decider uses only its root package, Kotlin, java time and java math, never a port or a clock`() {
        rule("domain.decider-is-pure") {
            description = "A decider decides from its arguments alone, and its use case does the I/O around it: it " +
                "uses only the types of its root package, com.drinkit here, and those of Kotlin, java.time and " +
                "java.math, never a port or a clock. Pass the values in"
            val forbidden = ports.map { it.fqName }.toSet() + setOf("java.time.Clock", "kotlin.time.Clock")
            classes {
                that().inModule(DOMAINS)
                that().annotatedWith("FunctionalCore")
                should().satisfy { cls, violations ->
                    val allowed = listOf("${rootPackageOf(cls.packageName)}.", "kotlin.", "java.time.", "java.math.")
                    cls.referencedTypeNames()
                        .filter { type -> type in forbidden || allowed.none(type::startsWith) }
                        .forEach { violations += "${cls.fqName} uses $it" }
                }
            }
        }.check()
    }

    @Test
    fun `a domain name says what a thing is, never its pattern`() {
        rule("domain.names-say-what-not-pattern") {
            description = "A domain name says what a thing is, never its pattern, so none ends in UseCase, " +
                "Service, Repository, Impl or Dto: a port is Cellars, a use case CreateCellar. Rename it"
            classes {
                that().inModule(DOMAINS)
                should().notNameEndsWith("UseCase", "Service", "Repository", "Impl", "Dto", "DTO")
            }
        }.check()
    }

    @Test
    fun `the domain reads no system clock and draws no identifier or random value itself`() {
        rule("domain.time-and-ids-from-providers") {
            description = "Time comes from the injected Clock, identifiers from GenerateId and randomness from a " +
                "port, so tests control them, never from Clock.system(), now() without a clock, " +
                "System.currentTimeMillis(), UUID.randomUUID() or Random: pass the clock to now(), and inject the " +
                "others"
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
}

// Konture records now() and now(clock) as the same call: only the text of the call tells them apart
private fun SourceUsage.callsNowWithoutClock() =
    kind == UsageKind.CALL && targetFqName.matches(Regex("""java\.time\.\w+\.now""")) &&
        File(filePath).readText().substring(sourceStartOffset, sourceEndOffset) == "now()"
