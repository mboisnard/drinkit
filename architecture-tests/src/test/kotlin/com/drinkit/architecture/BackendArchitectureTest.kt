package com.drinkit.architecture

import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class BackendArchitectureTest {

    @Test
    fun `a controller is internal and implements an interface generated from the contract`() {
        rule("backend.controllers-implement-generated-interfaces") {
            description = "The API contract defines every endpoint and generates its interface, which carries the " +
                "mappings: implement it in an internal @RestController named after it, CellarsController for " +
                "CellarsApi"
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
            description = "An endpoint the contract does not define escapes its review and the generated client: " +
                "add it to the API contract of the application and implement its interface"
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
    fun `the backend uses no port directly`() {
        rule("backend.calls-use-cases-never-ports") {
            description = "The backend turns a request or a message into a call to a use case, which holds the " +
                "rules and the transaction around the ports: call a use case instead of the port"
            suppress { file("*/com/drinkit/user/UserController.kt", FOLLOW_UP) }
            val portNames = ports.map { it.fqName }.toSet()
            files {
                that().inModules(BACKENDS)
                should().satisfy { file, violations ->
                    file.declaration.namesUsed()
                        .filter { it in portNames }
                        .forEach { violations += "${file.declaration.name} uses $it" }
                }
            }
        }.check()
    }

    @Test
    fun `a controller package is named after its bounded context`() {
        rule("backend.controller-package-named-after-context") {
            description = "A controller sits in the package of the bounded context it serves, so a context reads as " +
                "one package across the modules: move it to the package of its context, such as com.drinkit.cellar"
            suppress {
                classFqName("com.drinkit.cellars.CellarsController", FOLLOW_UP)
                classFqName("com.drinkit.bottles.search.SearchController", FOLLOW_UP)
            }
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

    @Test
    fun `the backend uses no class or function of infra`() {
        rule("backend.uses-nothing-of-infra") {
            description = "The backend reaches infra through the use cases only, which call the ports Spring wires " +
                "the adapters behind: call a use case instead of the adapter or its function"
            val infra = declarationsOf(INFRAS)
            files {
                that().inModules(BACKENDS)
                should().satisfy { file, violations ->
                    file.declaration.namesUsed()
                        .filter { it in infra }
                        .forEach { violations += "${file.declaration.name} uses $it" }
                }
            }
        }.check()
    }
}
