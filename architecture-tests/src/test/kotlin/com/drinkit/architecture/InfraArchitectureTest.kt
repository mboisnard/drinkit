package com.drinkit.architecture

import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class InfraArchitectureTest {

    @Test
    fun `jOOQ appears only in infra and in starters`() {
        rule("infra.jooq-only-in-infra-and-starters") {
            description = "SQL lives in the infra adapters and in the starters that own a table: move this jOOQ code " +
                "behind a port"
            files {
                that().inModules(APPLICATIONS - INFRAS)
                should().notDependOnPackages("org.jooq..")
            }
        }.check()
    }

    @Test
    fun `an adapter uses the core and spi types of the domain only`() {
        rule("infra.uses-domain-through-core-and-spi") {
            description = "The domain drives the adapters, never the reverse: an adapter uses only core, spi and " +
                "common types. Move the call into the use case, or the type into core"
            classes {
                that().inModule(INFRAS)
                that().areTopLevel()
                should().satisfy { adapter, violations ->
                    adapter.referencedDomainClasses()
                        .filter { it.role() == Role.USE_CASES }
                        .forEach { violations += "${adapter.fqName} uses ${it.fqName}" }
                }
            }
        }.check()
    }

    @Test
    fun `every class of infra is internal`() {
        rule("infra.classes-are-internal") {
            description = "Infra is reached through the ports only: make the class internal"
            classes {
                that().inModule(INFRAS)
                that().areTopLevel()
                should().beInternal()
            }
        }.check()
    }

    @Test
    fun `a jOOQ adapter is a jOOQ repository`() {
        rule("infra.jooq-adapter-is-jooq-repository") {
            description = "@JooqRepository demands a transaction and translates the database errors: annotate the " +
                "jOOQ adapter with it"
            classes {
                that().inModule(INFRAS)
                that().areTopLevel()
                that().satisfy { "org.jooq.DSLContext" in it.referencedTypeNames() }
                should().annotatedWith("JooqRepository")
            }
        }.check()
    }

    @Test
    fun `an adapter sits in the package of its port's context`() {
        rule("infra.adapter-in-context-of-its-port") {
            description = "A context reads as one package across the modules: move the adapter into the package of " +
                "its port's context"
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
    fun `an adapter is named after its technology and its port`() {
        rule("infra.adapter-named-after-port") {
            description = "An adapter is named after its technology, then its port or the port's last word: " +
                "JooqCellars for Cellars, EuropeanCentralBankProvider for ExchangeRateProvider"
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
}
