package com.drinkit.architecture

import io.github.baole.konture.ClassDeclaration
import io.github.baole.konture.SourceSets
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class TestConventionsArchitectureTest {

    @Test
    fun `tests use no mocking library`() {
        rule("tests.no-mocking-library") {
            description = "A test runs against hand-written doubles of src/testFixtures, never mocks, so it checks " +
                "behavior rather than calls: write an in-memory, spy or fake double instead"
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf("org.mockito..", "io.mockk..")
            }
        }.check()
    }

    @Test
    fun `tests assert with Kotest only`() {
        rule("tests.kotest-assertions-only") {
            description = "Tests assert with Kotest only, never AssertJ, Hamcrest, kotlin.test or JUnit's " +
                "assertions, so failures read the same everywhere: replace the assertion with its Kotest matcher"
            suppress {
                file("*/com/drinkit/configuration/infra/ConfigurationsTestContract.kt", FOLLOW_UP)
                file("*/com/drinkit/user/CompleteProfileInformationTest.kt", FOLLOW_UP)
            }
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
    fun `a test is named by a sentence in backticks`() {
        rule("tests.backticked-names") {
            description = "A test name is a sentence in backticks stating the behavior, so the test names read as " +
                "the specification: rename it"
            functions(SourceSets.tests()) {
                that().resideInModules(ALL)
                that().haveAnnotationOf("Test")
                should().haveNameMatching("* *")
            }
        }.check()
    }

    @Test
    fun `no test is disabled`() {
        rule("tests.no-disabled-test") {
            description = "A disabled test checks nothing while it looks covered: fix it, or delete it"
            suppress { file("*/com/drinkit/ocr/analyzers/TesseractDocumentAnalyserTest.kt", OPS_STARTERS) }
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf("org.junit.jupiter.api.Disabled")
            }
        }.check()
    }

    @Test
    fun `a port has a test contract that its double and its adapter both pass`() {
        rule("tests.port-has-test-contract") {
            description = "A port's behavior is written once, in an abstract <Port>TestContract beside the port in " +
                "the domain test fixtures, which its in-memory double and its real adapter both extend: write it, " +
                "or extend it"
            suppress {
                classFqName("com.drinkit.user.spi.VerificationTokens", FOLLOW_UP)
                classFqName("com.drinkit.user.spi.Users", FOLLOW_UP)
                classFqName("com.drinkit.common.MessageSender", FOLLOW_UP)
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
    fun `a double lives in the test fixtures and is named after its kind`() {
        rule("tests.doubles-named-after-their-kind") {
            description = "A double of a port is shared from src/testFixtures and says what it is: InMemory, Spy or " +
                "Fake, never Mock, since nothing is mocked. Move or rename it"
            suppress {
                classFqName("com.drinkit.common.MockGenerateId", FOLLOW_UP)
                classFqName("com.drinkit.featureflags.MockFeatureFlags", OPS_STARTERS)
            }
            val portNames = ports.map { it.fqName }.toSet()
            classes(SourceSets.tests()) {
                that().inModule(ALL)
                that().satisfy { cls -> cls.supertypes.any { it in portNames } }
                should().nameStartsWith("InMemory", "Spy", "Fake")
                andShould().satisfy { double, violations ->
                    if (double.file().sourceSets.none { it.name == "testFixtures" }) {
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
    fun `a jOOQ adapter is tested against a real database`() {
        rule("tests.jooq-adapter-test-is-integration-test") {
            description = "A jOOQ adapter is tested against PostgreSQL, through @JooqIntegrationTest and the " +
                "committed jOOQ schema: annotate the test"
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
    fun `tests build their data through the domain`() {
        rule("tests.no-sql-data-setup") {
            description = "A test builds its data through the domain and its fixtures, which follow every schema " +
                "change, never through SQL scripts: replace @Sql with fixtures"
            files(SourceSets.tests()) {
                that().inModules(ALL)
                should().notHaveImportOf("org.springframework.test.context.jdbc..")
            }
        }.check()
    }
}

// Konture leaves a supertype of the same package, which needs no import, under its simple name
private fun ClassDeclaration.extends(other: ClassDeclaration) =
    other.fqName in supertypes || (other.name in supertypes && packageName == other.packageName)
