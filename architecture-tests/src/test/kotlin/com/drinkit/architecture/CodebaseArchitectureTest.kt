package com.drinkit.architecture

import io.github.baole.konture.Modifier
import io.github.baole.konture.SourceSets
import io.github.baole.konture.UsageKind
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class CodebaseArchitectureTest {

    @Test
    fun `an application uses no module of another application`() {
        rule("codebase.applications-are-independent") {
            description = "Each application builds and ships alone: reach another one through its API or its " +
                "messages, or move the shared technical code into a tech starter"
            modules {
                that().haveNamePath(APPLICATIONS)
                should().satisfy { module, violations ->
                    module.dependencies
                        .filter { it.targetPath in APPLICATIONS }
                        .filter { applicationOf(it.targetPath) != applicationOf(module.path) }
                        .forEach { violations += "${module.path} depends on ${it.targetPath} via ${it.configuration}" }
                }
            }
        }.check()
    }

    @Test
    fun `a package under the root belongs to one application or to the tech starters`() {
        rule("codebase.package-has-one-owner") {
            description = "A package right under the root belongs to one application or to the tech starters, never " +
                "to two: rename the context, or the package of the starter"
            val modulesByPackage = classesOf(ALL, SourceSets.production() or SourceSets.tests())
                .filter { it.packageName != rootPackageOf(it.packageName) }
                .groupBy({ contextOf(it.packageName) }, { it.module() })
                .mapValues { it.value.toSet() }
            fun ownerOf(module: String) = if (module in STARTERS) "starters" else applicationOf(module)
            modules {
                that().haveNamePath(ALL)
                should().satisfy { module, violations ->
                    modulesByPackage
                        .filter { (_, modules) -> module.path in modules && modules.map(::ownerOf).toSet().size > 1 }
                        .forEach { (name, modules) ->
                            val others = (modules - module.path).sorted()
                            violations += "${module.path} shares $name with $others"
                        }
                }
            }
        }.check()
    }

    @Test
    fun `an application calls HTTP through registered declarative clients`() {
        rule("codebase.http-through-registered-clients") {
            description = "Only a tech starter builds HTTP requests by hand: replace RestTemplate, RestClient, " +
                "WebClient or java.net.http with an @HttpExchange interface registered by @ImportHttpServices"
            files {
                that().inModules(APPLICATIONS)
                should().notReferenceClass("org.springframework.web.client.RestTemplate")
                andShould().notReferenceClass("org.springframework.web.client.RestClient")
                andShould().notReferenceClass("org.springframework.web.reactive.function.client.WebClient")
                andShould().notReferenceClass("java.net.http.HttpClient")
            }
            // The words of the @ImportHttpServices annotations: [EuropeanCentralBankClient::class] gives
            // EuropeanCentralBankClient and class
            val registered = classesOf(APPLICATIONS)
                .flatMap { cls -> cls.annotations.filter { it.name == "ImportHttpServices" } }
                .flatMap { annotation -> annotation.arguments.flatMap { it.value.split(Regex("""\W+""")) } }
                .toSet()
            classes {
                that().inModule(APPLICATIONS)
                that().satisfy { it.isHttpClient() }
                should().satisfy { client, violations ->
                    if (client.name !in registered) {
                        violations += "${client.fqName} is not registered with @ImportHttpServices"
                    }
                }
            }
        }.check()
    }

    @Test
    fun `no code uses Date or Calendar`() {
        rule("codebase.no-legacy-date") {
            description = "Date and Calendar are mutable and carry no zone: use Instant, OffsetDateTime or LocalDate"
            files {
                that().inModules(ALL)
                should().notReferenceClass("java.util.Date")
                andShould().notCall("java.util.Date")
                andShould().notReferenceClass("java.util.Calendar")
            }
        }.check()
    }

    @Test
    fun `a class declares no operator invoke`() {
        rule("codebase.no-operator-invoke-in-class") {
            description = "A call such as createCellar(command) hides the function it runs: drop operator from the " +
                "invoke of a class and call invoke by its name. An object, and the other operators, may keep it"
            classes(SourceSets.production() or SourceSets.tests()) {
                that().inModule(ALL)
                that().satisfy { Modifier.OBJECT !in it.modifiers && Modifier.COMPANION !in it.modifiers }
                should().satisfy { cls, violations ->
                    cls.functions
                        .filter { it.name == "invoke" && Modifier.OPERATOR in it.modifiers }
                        .forEach { violations += "${cls.fqName}.invoke is an operator" }
                }
            }
        }.check()
    }

    @Test
    fun `a logger is a private KotlinLogging property named logger`() {
        rule("codebase.logger-convention") {
            description = "Every file declares its logger the same way: private val logger = KotlinLogging.logger " +
                "{}, never a logger of SLF4J, Log4j or java.util.logging"
            // A KotlinLogging logger is named logger. Konture's selection by dependency picks the wrong class in a
            // file of several, so the rule reads the other way: a property named otherwise is no KotlinLogging logger
            properties {
                that().resideInModules(ALL)
                that().notHaveName("logger")
                should().notCall("io.github.oshai.kotlinlogging.KotlinLogging.logger")
            }
            properties {
                that().resideInModules(ALL)
                that().haveName("logger")
                should().bePrivate()
            }
            // A logger of another library has nothing else to tell it, whatever its name, but its factory
            files {
                that().inModules(ALL)
                should().satisfy { file, violations ->
                    file.declaration.usages
                        .filter { it.kind == UsageKind.CALL && it.targetFqName.endsWith(".getLogger") }
                        .forEach { violations += "${file.declaration.name} calls ${it.rawExpression}()" }
                }
            }
        }.check()
    }
}
