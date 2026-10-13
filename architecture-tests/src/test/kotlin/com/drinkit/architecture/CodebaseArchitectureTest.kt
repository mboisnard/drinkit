package com.drinkit.architecture

import io.github.baole.konture.ClassDeclaration
import io.github.baole.konture.UsageKind
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class CodebaseArchitectureTest {

    @Test
    fun `an application uses no module of another application`() {
        rule("codebase.applications-are-independent") {
            description = "An application depends on no module of another application, so that each one builds, " +
                "tests and ships alone: reach the other application through its API or its messages, or move the " +
                "technical code into a tech starter"
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
    fun `an application calls HTTP through registered declarative clients`() {
        rule("codebase.http-through-registered-clients") {
            description = "An application never builds an HTTP request by hand with RestTemplate, RestClient, " +
                "WebClient or java.net.http, which only a tech starter does: declare an @HttpExchange interface and " +
                "register it with @ImportHttpServices"
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
            description = "Dates are java.time types, which carry their zone and stay immutable: replace Date or " +
                "Calendar with Instant, OffsetDateTime or LocalDate"
            files {
                that().inModules(ALL)
                should().notReferenceClass("java.util.Date")
                andShould().notCall("java.util.Date")
                andShould().notReferenceClass("java.util.Calendar")
            }
        }.check()
    }

    @Test
    fun `a logger is a private KotlinLogging property named logger`() {
        rule("codebase.logger-convention") {
            description = "A logger is declared one way, private val logger = KotlinLogging.logger {}, so it reads " +
                "the same in every file, and never comes from the getLogger() of SLF4J, Log4j or java.util.logging: " +
                "rename it, make it private, or create it with KotlinLogging"
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

// An HTTP client declares its requests with @GetExchange, @PostExchange or another @*Exchange
private fun ClassDeclaration.isHttpClient() =
    functions.any { function -> function.annotations.any { it.name.endsWith("Exchange") } }
