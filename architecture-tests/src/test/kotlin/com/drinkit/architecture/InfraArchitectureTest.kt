package com.drinkit.architecture

import io.github.baole.konture.hasAnnotation
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class InfraArchitectureTest {

    @Test
    fun `every class of infra is internal`() {
        rule("infra.classes-are-internal") {
            description = "Infra is reached through the ports only, which Kotlin enforces once its classes are " +
                "internal: make the class internal"
            classes {
                that().inModule(INFRAS)
                that().areTopLevel()
                should().beInternal()
            }
        }.check()
    }

    @Test
    fun `an adapter is named after its technology and its port`() {
        rule("infra.adapter-named-after-port") {
            description = "An adapter's name starts with its technology or source and ends with the port it " +
                "implements, or the last word of that port: JooqCellars for Cellars, EuropeanCentralBankProvider " +
                "for ExchangeRateProvider. Rename it"
            suppress {
                classFqName("com.drinkit.user.JooqUsersRepository", FOLLOW_UP)
                classFqName("com.drinkit.user.JooqUserEventsRepository", FOLLOW_UP)
                classFqName("com.drinkit.user.JooqVerificationTokensRepository", FOLLOW_UP)
                classFqName("com.drinkit.money.forex.JooqExchangeRatesRepository", FOLLOW_UP)
            }
            classes {
                that().inModule(INFRAS)
                that().satisfy { it in adapters }
                should().satisfy { adapter, violations ->
                    val ports = adapters.getValue(adapter)
                    if (ports.none { adapter.name.namesPort(it.name) }) {
                        violations += "${adapter.fqName} is not named after ${ports.joinToString { it.name }}"
                    }
                }
            }
        }.check()
    }

    @Test
    fun `a jOOQ adapter is a jOOQ repository`() {
        rule("infra.jooq-adapter-is-jooq-repository") {
            description = "A jOOQ adapter is a @JooqRepository, which demands a running transaction and translates " +
                "the database errors: annotate it"
            classes {
                that().inModule(INFRAS)
                that().areTopLevel()
                that().satisfy { "org.jooq.DSLContext" in it.referencedTypeNames() }
                should().annotatedWith("JooqRepository")
            }
        }.check()
    }

    @Test
    fun `jOOQ appears only in infra and in starters`() {
        rule("infra.jooq-only-in-infra-and-starters") {
            description = "SQL lives in the adapters of the infra modules and in the starters that own a table: " +
                "move this code behind a port"
            files {
                that().inModules(APPLICATIONS - INFRAS)
                should().notDependOnPackages("org.jooq..")
            }
        }.check()
    }

    @Test
    fun `an adapter sits in the package of its port's context`() {
        rule("infra.adapter-in-context-of-its-port") {
            description = "An adapter sits in the package of the context its port belongs to, so a context reads as " +
                "one package across the modules: move it there"
            suppress { classFqName("com.drinkit.message.sender.EmailMessageSender", FOLLOW_UP) }
            classes {
                that().inModule(INFRAS)
                that().satisfy { it in adapters }
                should().satisfy { adapter, violations ->
                    adapters.getValue(adapter)
                        .filter { contextOf(it.packageName) != contextOf(adapter.packageName) }
                        .forEach { violations += "${adapter.fqName} implements ${it.fqName}" }
                }
            }
        }.check()
    }

    @Test
    fun `an adapter uses no service of the domain`() {
        rule("infra.adapter-uses-no-domain-service") {
            description = "The domain drives the adapters, never the other way round: an adapter uses no @Service or " +
                "@Component of the domain, use case or not. Move the call into the use case that calls the port"
            val services = classesOf(DOMAINS)
                .filter { it.hasAnnotation("Service") || it.hasAnnotation("Component") }
                .map { it.fqName }
                .toSet()
            classes {
                that().inModule(INFRAS)
                that().areTopLevel()
                should().satisfy { adapter, violations ->
                    adapter.referencedTypeNames()
                        .filter { it in services }
                        .forEach { violations += "${adapter.fqName} uses $it" }
                }
            }
        }.check()
    }
}

// A technology or a source, then the port or its last word: JooqCellars, EuropeanCentralBankProvider
private fun String.namesPort(port: String): Boolean {
    val lastWord = port.substring(port.indexOfLast { it.isUpperCase() })
    return listOf(port, lastWord).any { length > it.length && endsWith(it) }
}
