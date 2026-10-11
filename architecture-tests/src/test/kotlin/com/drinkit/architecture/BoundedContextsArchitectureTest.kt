package com.drinkit.architecture

import io.github.baole.konture.architecture
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

// A domain module without code yet has no slice to check
private val domainsWithCode = DOMAINS.filter { classesOf(listOf(it)).isNotEmpty() }

// A slice pattern starts with the root package of the domain, com.drinkit
private fun rootOf(domain: String) = rootPackageOf(classesOf(listOf(domain)).first().packageName)

internal class BoundedContextsArchitectureTest {

    @Test
    fun `bounded contexts depend on each other without a cycle`() {
        domainsWithCode.forEach { domain ->
            architecture(graphOf(domain)) {
                rule("contexts.no-cycle-between-contexts") {
                    description = "A bounded context may use another, never the other way round as well: move what " +
                        "both need into the one that owns it, or into common"
                    slices {
                        matching("${rootOf(domain)}.(*)..")
                        // A rule below already forbids every dependency of common on a context
                        that().notHaveKey("common")
                        should().beFreeOfCycles()
                    }
                }
            }
        }
    }

    @Test
    fun `the packages of the domain depend on each other without a cycle`() {
        domainsWithCode.forEach { domain ->
            architecture(graphOf(domain)) {
                rule("contexts.no-cycle-between-packages") {
                    description = "The packages of a domain depend on each other in one direction only, so that each " +
                        "one reads and changes without the others: break the cycle by moving the type to the " +
                        "package the others depend on"
                    suppress { slice("money.forex.core", FOLLOW_UP) }
                    slices {
                        matching("${rootOf(domain)}.(**)")
                        that().notHaveKey("common")
                        should().beFreeOfCycles()
                    }
                }
            }
        }
    }

    @Test
    fun `common depends on no bounded context`() {
        rule("contexts.common-depends-on-no-context") {
            description = "common holds what every context shares, so it knows none of them: move the type into the " +
                "context it depends on, or the dependency into the context"
            suppress { classFqName("com.drinkit.common.Author", FOLLOW_UP) }
            classes {
                that().inModule(DOMAINS)
                that().satisfy { contextOf(it.packageName).endsWith(".common") }
                that().areTopLevel()
                should().notDependOnClassesInAnyPackage(contexts.filterNot { it.endsWith(".common") }.map { "$it.." })
            }
        }.check()
    }
}
