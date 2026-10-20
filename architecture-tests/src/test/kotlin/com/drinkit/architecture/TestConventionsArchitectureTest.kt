package com.drinkit.architecture

import io.github.baole.konture.SourceSets
import io.github.baole.konture.Visibility
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class TestConventionsArchitectureTest {

    @Test
    fun `tests use no mocking library`() {
        rule("tests.no-mocking-library") {
            description = "A test checks behavior through hand-written doubles, not mocks: replace Mockito or MockK " +
                "with an InMemory, Spy or Fake double of src/testFixtures"
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf("org.mockito..", "io.mockk..")
            }
        }.check()
    }

    @Test
    fun `tests assert with Kotest only`() {
        rule("tests.kotest-assertions-only") {
            description = "Failures read the same in every test: replace the AssertJ, Hamcrest, kotlin.test or JUnit " +
                "assertion with its Kotest matcher"
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf(
                    "org.assertj..",
                    "org.hamcrest..",
                    "kotlin.test..",
                    "org.junit.Assert..",
                    "org.junit.jupiter.api.Assertions..",
                    "org.junit.jupiter.api.assertThrows",
                    "org.junit.jupiter.api.assertAll",
                )
            }
        }.check()
    }

    @Test
    fun `tests build their data through the domain`() {
        rule("tests.no-sql-data-setup") {
            description = "An SQL script drifts from the schema: replace @Sql with data built through the domain and " +
                "its fixtures"
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf("org.springframework.test.context.jdbc..")
            }
        }.check()
    }

    @Test
    fun `a port has a test contract that its double and its adapter both pass`() {
        rule("tests.port-has-test-contract") {
            description = "A port's behavior is written once, in an abstract <Port>TestContract of the domain test " +
                "fixtures: write it, and extend it from the in-memory double's test and the adapter's test"
            suppress {
                classFqName("com.drinkit.user.spi.VerificationTokens", FOLLOW_UP)
                classFqName("com.drinkit.user.spi.Users", FOLLOW_UP)
                classFqName("com.drinkit.notification.spi.NotificationSender", FOLLOW_UP)
                classFqName("com.drinkit.common.GenerateId", FOLLOW_UP)
                classFqName("com.drinkit.money.forex.spi.ExchangeRateProvider", FOLLOW_UP)
                classFqName("com.drinkit.cellar.spi.Cellars", FOLLOW_UP)
                classFqName("com.drinkit.bottle.spi.Bottles", FOLLOW_UP)
            }
            val doubleTests = classesOf(DOMAINS, SourceSets.tests())
            val adapterTests = classesOf(INFRAS, SourceSets.tests())
            classes {
                that().inModule(DOMAINS)
                that().satisfy { it in ports }
                should().satisfy { port, violations ->
                    val contract = doubleTests.firstOrNull {
                        it.fqName == "${port.fqName}TestContract" && it.isAbstract
                    }
                    when {
                        contract == null -> violations += "${port.fqName} has no ${port.name}TestContract"

                        contract.functions.none { function -> function.annotations.any { it.name == "Test" } } ->
                            violations += "${contract.fqName} has no test"

                        doubleTests.none { it.extends(contract) } -> violations += "${contract.fqName} runs no double"

                        adapterTests.none { it.extends(contract) } -> violations += "${contract.fqName} runs no adapter"
                    }
                }
            }
        }.check()
    }

    @Test
    fun `a jOOQ adapter is tested against a real database`() {
        rule("tests.jooq-adapter-test-is-integration-test") {
            description = "A jOOQ adapter is tested against PostgreSQL: annotate its test @JooqIntegrationTest"
            classes(SourceSets.tests()) {
                that().inModule(ALL)
                that().nameStartsWith("Jooq")
                that().nameEndsWith("Test")
                that().notNamed("JooqIntegrationTest")
                should().annotatedWith("JooqIntegrationTest")
            }
        }.check()
    }

    @Test
    fun `no test is disabled`() {
        rule("tests.no-disabled-test") {
            description = "A disabled test looks covered but checks nothing: fix it, or delete it"
            suppress { file("*/com/drinkit/ocr/analyzers/TesseractDocumentAnalyserTest.kt", OPS_STARTERS) }
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf("org.junit.jupiter.api.Disabled")
            }
        }.check()
    }

    @Test
    fun `a test class is internal`() {
        rule("tests.classes-are-internal") {
            description = "A test class serves its own module only: make it internal or private. What other modules' " +
                "tests share goes in src/testFixtures"
            classes(SourceSets.tests() and !SourceSets.named("testFixtures")) {
                that().inModule(ALL)
                that().areTopLevel()
                should().haveAnyVisibility(Visibility.INTERNAL, Visibility.PRIVATE)
            }
        }.check()
    }

    @Test
    fun `a double lives in the test fixtures and is named after its kind`() {
        rule("tests.doubles-named-after-their-kind") {
            description = "A double of a port lives in src/testFixtures and says what it is: name it InMemory, Spy " +
                "or Fake, never Mock"
            classes(SourceSets.tests()) {
                that().inModule(ALL)
                // Konture's areAssignableToAnyOf also selects tests that implement no port, such as JooqUserEventsTest
                that().satisfy { cls -> cls.supertypes.any { it in portNames } }
                should().nameStartsWith("InMemory", "Spy", "Fake")
                andShould().satisfy { double, violations ->
                    if ("/src/testFixtures/" !in double.filePath) {
                        violations += "${double.fqName} is not in src/testFixtures"
                    }
                }
            }
            classes(SourceSets.tests()) {
                that().inModule(ALL)
                should().notNameStartsWith("Mock")
            }
        }.check()
    }

    @Test
    fun `a test is named by a sentence in backticks`() {
        rule("tests.backticked-names") {
            description = "Test names read as the specification: name the test by a sentence in backticks"
            functions(SourceSets.tests()) {
                that().resideInModules(ALL)
                that().haveAnnotationOf("Test")
                should().haveNameMatching("* *")
            }
        }.check()
    }
}
