package com.drinkit.architecture

import io.github.baole.konture.SourceSets
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

// A jOOQ table import names its table, its record or its reference: ...tables.User, tables.records.UserRecord,
// tables.references.USER
private val TABLE_IMPORT = Regex("""\.generated\.jooq\.tables\.(?:records\.|references\.)?(\w+)""")

private fun String.firstWord() = substringBefore('_').split(Regex("(?<=[a-z])(?=[A-Z])")).first().lowercase()

internal class TechStartersArchitectureTest {

    @Test
    fun `a tech starter depends on no application`() {
        rule("starters.depend-on-no-application") {
            description = "A tech starter works without business code: replace the dependency on an application " +
                "module with a port that the application implements"
            modules {
                that().haveNamePath(STARTERS)
                should().notDependOn(APPLICATIONS)
            }
        }.check()
    }

    @Test
    fun `a tech starter reads no table of a bounded context`() {
        rule("starters.read-no-business-table") {
            description = "A starter that reads a table of a context, such as USER, ties itself to the schema of one " +
                "application: ask the application for the data through a port"
            val contextNames = contexts.filterNot { it.endsWith(".common") }.map { it.substringAfterLast('.') }
            files {
                that().inModules(STARTERS)
                should().satisfy { file, violations ->
                    file.declaration.imports
                        .mapNotNull { TABLE_IMPORT.find(it)?.groupValues?.get(1) }
                        .filter { it.firstWord() in contextNames }
                        .forEach { violations += "${file.declaration.name} reads $it of a bounded context" }
                }
            }
        }.check()
    }

    @Test
    fun `only the public API of a tech starter is public, and it says so`() {
        rule("starters.public-api-is-marked") {
            description = "A tech starter documents its public API with @TechStarterTool and keeps the rest " +
                "internal: mark the class, or make it internal. A data or value class is a value that a tool " +
                "takes, and needs no mark"
            classes {
                that().inModule(STARTERS)
                that().areTopLevel()
                that().bePublic()
                not().beData()
                not().beInline()
                should().annotatedWith("TechStarterTool")
            }
            classes {
                that().inModule(STARTERS)
                that().annotatedWith("TechStarterTool")
                should().bePublic()
            }
        }.check()
    }

    @Test
    fun `a tech starter registers its beans in its auto-configuration, not through the application's scan`() {
        rule("starters.no-stereotype") {
            description = "The scan of an application starts above the packages of its starters, so it registers a " +
                "@Component, a @RestController or a nested @Configuration of a starter on its own: that bean ignores " +
                "starters.<name>.enabled and @ConditionalOnMissingBean, and is gone in an application of another " +
                "root package. Declare the bean in the starter's @AutoConfiguration"
            val stereotypes = setOf(
                "Component",
                "Service",
                "Repository",
                "Controller",
                "RestController",
                "Configuration",
                "JooqRepository",
            )
            classes {
                that().inModule(STARTERS)
                // JooqRepository, built on @Repository, marks the beans of the application, not the starter's
                that().notNamed("JooqRepository")
                should().satisfy { cls, violations ->
                    cls.annotations
                        .filter { it.name in stereotypes }
                        .forEach { violations += "${cls.fqName} is a @${it.name}" }
                }
            }
        }.check()
    }

    @Test
    fun `a bean of a tech starter backs off when the application defines its own`() {
        rule("starters.beans-back-off") {
            description = "The application's own bean wins over a starter's: annotate the @Bean " +
                "@ConditionalOnMissingBean"
            functions {
                that().resideInModules(STARTERS)
                that().haveAnnotationOf("Bean")
                should().haveAnnotationOf("ConditionalOnMissingBean")
            }
        }.check()
    }

    @Test
    fun `the settings of a tech starter live under starters`() {
        rule("starters.settings-under-starters-prefix") {
            description = "Settings of a tech starter never clash with Spring's or the application's: put them under " +
                "starters.<name>"
            classes {
                that().inModule(STARTERS)
                that().annotatedWith("ConfigurationProperties")
                should().satisfy { cls, violations ->
                    val annotation = cls.annotations.first { it.name == "ConfigurationProperties" }
                    val prefix = annotation.arguments.firstOrNull()?.value.orEmpty().trim('"')
                    if (!prefix.startsWith("starters.")) violations += "${cls.fqName} binds $prefix"
                }
            }
        }.check()
    }

    @Test
    fun `the packages of a tech starter depend on each other without a cycle`() {
        val starters = STARTERS.map(::listOf).filter { classesOf(it).isNotEmpty() }

        // One failure per starter, all of them at once
        assertAll(
            starters.map { starter ->
                {
                    rule("starters.no-cycle-between-packages") {
                        description = "The packages of a tech starter depend on each other one way only: move the " +
                            "type that closes the cycle into the package the others use"
                        slices(sourceSetsOf(starter)) {
                            matching("${rootPackageOf(classesOf(starter).first().packageName)}.(**)")
                            not().haveKey { "generated" in it.split('.') }
                            should().beFreeOfCycles()
                        }
                    }.check()
                }
            },
        )
    }

    @Test
    fun `the package of a tech starter is named after the module`() {
        rule("starters.package-named-after-module") {
            description = "A tech starter's code, test fixtures included, lives in the package named after its " +
                "module, com.drinkit.event.sourcing for event-sourcing-starter: rename the package or the module"
            files(SourceSets.production() or SourceSets.tests()) {
                that().inModules(STARTERS)
                should().satisfy { file, violations ->
                    val packageName = file.declaration.packageName
                    val name = file.modulePath.substringAfterLast(':').removeSuffix("-starter")
                    val expected = listOf(name.replace("-", ""), name.replace('-', '.'))
                        .map { "${rootPackageOf(packageName)}.$it" }
                    if (expected.none { "$packageName.".startsWith("$it.") }) {
                        violations += "${file.modulePath} ${file.declaration.name} is in $packageName"
                    }
                }
            }
        }.check()
    }
}
