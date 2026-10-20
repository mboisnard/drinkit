package com.drinkit.architecture

import io.github.baole.konture.Visibility
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class BackendArchitectureTest {

    @Test
    fun `a backend class that uses a port needing a transaction runs in one`() {
        rule("backend.port-used-in-transaction") {
            description = "A port whose adapter is a @JooqRepository needs a running transaction: annotate the class " +
                "that uses it, or each of its non-private methods, @Transactional or @RetryableTransactional"
            val portNames = transactionalPorts.map { it.fqName }.toSet()
            val transactional = setOf("Transactional", "RetryableTransactional")
            classes {
                that().inModule(BACKENDS)
                that().satisfy { cls -> cls.referencedTypeNames().any { it in portNames } }
                should().satisfy { cls, violations ->
                    val methods = cls.functions.filter { it.visibility != Visibility.PRIVATE }
                    val classInTransaction = cls.annotations.any { it.name in transactional }
                    val methodsInTransaction = methods.isNotEmpty() &&
                        methods.all { method -> method.annotations.any { it.name in transactional } }
                    if (!classInTransaction && !methodsInTransaction) {
                        violations += "${cls.fqName} uses a port outside a transaction"
                    }
                }
            }
        }.check()
    }

    @Test
    fun `a controller is internal and implements an interface generated from the contract`() {
        rule("backend.controllers-implement-generated-interfaces") {
            description = "The contract generates each endpoint's interface: implement it in an internal " +
                "@RestController named after it, CellarsController for CellarsApi"
            classes {
                that().inModule(BACKENDS)
                that().annotatedWith("RestController")
                should().beInternal()
                andShould().satisfy { controller, violations ->
                    val generated = "${controller.name.removeSuffix("Controller")}Api"
                    if (controller.supertypes.none { it.substringAfterLast('.') == generated }) {
                        violations += "${controller.fqName} does not implement $generated"
                    }
                }
            }
            classes {
                that().inModule(BACKENDS)
                that().satisfy { cls -> cls.supertypes.any { ".generated." in it && it.endsWith("Api") } }
                should().annotatedWith("RestController")
            }
        }.check()
    }

    @Test
    fun `no endpoint of the backend is mapped by hand`() {
        rule("backend.no-endpoint-mapped-by-hand") {
            description = "An endpoint outside the contract escapes its review and the generated client: add it to " +
                "the contract and implement the generated interface"
            val byHand = setOf(
                "RequestMapping",
                "GetMapping",
                "PostMapping",
                "PutMapping",
                "PatchMapping",
                "DeleteMapping",
                "Controller",
            )
            classes {
                that().inModule(BACKENDS)
                should().satisfy { cls, violations ->
                    (cls.annotations + cls.functions.flatMap { it.annotations })
                        .filter { it.name in byHand }
                        .forEach { violations += "${cls.fqName} uses @${it.name}" }
                }
            }
        }.check()
    }

    @Test
    fun `a controller package is named after its bounded context`() {
        rule("backend.controller-package-named-after-context") {
            description = "A context reads as one package across the modules: move the controller into the package " +
                "of its context, such as com.drinkit.cellar"
            classes {
                that().inModule(BACKENDS)
                that().annotatedWith("RestController")
                should().satisfy { controller, violations ->
                    val context = contextOf(controller.packageName)
                    if (context !in contextsByApplication[applicationOf(controller.module())].orEmpty()) {
                        violations += "${controller.fqName} sits in $context, not a context of its application"
                    }
                }
            }
        }.check()
    }
}
