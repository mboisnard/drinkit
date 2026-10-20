package com.drinkit.architecture

import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class BoundedContextsArchitectureTest {

    @Test
    fun `bounded contexts depend on each other without a cycle`() {
        domainsWithCode.forEach { domain ->
            rule("contexts.no-cycle-between-contexts") {
                description = "Two contexts never depend on each other both ways: move what both need into the one " +
                    "that owns it, or into common"
                slices(sourceSetsOf(listOf(domain))) {
                    matching("${rootOf(domain)}.(*)..")
                    // A rule below already forbids every dependency of common on a context
                    that().notHaveKey("common")
                    should().beFreeOfCycles()
                }
            }.check()
        }
    }

    @Test
    fun `the packages of the domain depend on each other without a cycle`() {
        domainsWithCode.forEach { domain ->
            rule("contexts.no-cycle-between-packages") {
                description = "The packages of a domain depend on each other one way only: move the type that closes " +
                    "the cycle into the package the others use"
                slices(sourceSetsOf(listOf(domain))) {
                    matching("${rootOf(domain)}.(**)")
                    that().notHaveKey("common")
                    should().beFreeOfCycles()
                }
            }.check()
        }
    }

    @Test
    fun `common depends on no bounded context`() {
        rule("contexts.common-depends-on-no-context") {
            description = "Every context uses common, so common uses none of them: move the type into the context it " +
                "depends on"
            classes {
                that().inModule(DOMAINS)
                that().satisfy { contextOf(it.packageName).endsWith(".common") }
                that().areTopLevel()
                should().notDependOnClassesInAnyPackage(contexts.filterNot { it.endsWith(".common") }.map { "$it.." })
            }
        }.check()
    }
}
