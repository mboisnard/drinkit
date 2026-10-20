package com.drinkit.architecture

import io.github.baole.konture.SourceSets
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

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
            suppress { file("*/com/drinkit/security/userdetails/UserDetailsRepository.kt", STARTER_TABLES) }
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
                "internal: mark the class, or make it internal"
            suppress {
                classFqName("com.drinkit.configuration.ConfigurationKey", DATA_STARTERS)
                classFqName("com.drinkit.retryable.LoggingRetryListener", DATA_STARTERS)
                classFqName("com.drinkit.security.configuration.SecurityConfiguration", WEB_STARTERS)
                classFqName("com.drinkit.security.AuthenticationFailedException", WEB_STARTERS)
                classFqName("com.drinkit.security.userdetails.InternalUserDetails", WEB_STARTERS)
                classFqName("com.drinkit.security.filter.MaintenanceModeFilter", WEB_STARTERS)
                classFqName("com.drinkit.sse.SseApi", WEB_STARTERS)
                classFqName("com.drinkit.mail.Email", MESSAGING_STARTERS)
                classFqName("com.drinkit.mail.Sender", MESSAGING_STARTERS)
                classFqName("com.drinkit.mail.Recipients", MESSAGING_STARTERS)
                classFqName("com.drinkit.mail.Subject", MESSAGING_STARTERS)
                classFqName("com.drinkit.mail.Content", MESSAGING_STARTERS)
                classFqName("com.drinkit.mail.Attachments", MESSAGING_STARTERS)
                classFqName("com.drinkit.messaging.events.SendSseEvent", MESSAGING_STARTERS)
                classFqName("com.drinkit.featureflags.FeatureFlagContext", OPS_STARTERS)
                classFqName("com.drinkit.ocr.OCRResponse", OPS_STARTERS)
                classFqName("com.drinkit.documentation.cqrs.Command", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.cqrs.Query", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.fcis.FunctionalCore", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.fcis.ImperativeShell", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.clean.architecture.CoreDomain", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.clean.architecture.Usecase", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.tech.starter.TechStarterTool", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.event.sourcing.Projection", LIBRARY_STARTERS)
                classFqName("com.drinkit.documentation.event.sourcing.Aggregate", LIBRARY_STARTERS)
                classFqName("com.drinkit.event.sourcing.transaction.DuplicateSequenceException", LIBRARY_STARTERS)
                classFqName("com.drinkit.event.sourcing.transaction.RetryableTransactional", LIBRARY_STARTERS)
                classFqName("com.drinkit.event.sourcing.DomainEvent", LIBRARY_STARTERS)
                classFqName("com.drinkit.event.sourcing.SequenceId", LIBRARY_STARTERS)
            }
            classes {
                that().inModule(STARTERS)
                that().areTopLevel()
                that().bePublic()
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
    fun `a tech starter wires its beans without stereotype annotations`() {
        rule("starters.no-stereotype") {
            description = "Only a scan of the starter's packages finds a @Component or @Service: declare the bean in " +
                "the starter's @AutoConfiguration"
            suppress {
                classFqName("com.drinkit.configuration.infra.JooqConfigurationsRepository", DATA_STARTERS)
                classFqName("com.drinkit.configuration.infra.ConfigurationSpringConfig", DATA_STARTERS)
                classFqName("com.drinkit.configuration.infra.CachedConfigurationsRepository", DATA_STARTERS)
                classFqName("com.drinkit.postgresql.jooq.JooqConfig", DATA_STARTERS)
                classFqName("com.drinkit.retryable.LoggingRetryListener", DATA_STARTERS)
                classFqName("com.drinkit.search.engine.MeilisearchConfig", DATA_STARTERS)
                classFqName("com.drinkit.security.password.encoder.InternalUserPasswordEncoder", WEB_STARTERS)
                classFqName("com.drinkit.security.InternalAuthenticationService", WEB_STARTERS)
                classFqName("com.drinkit.security.userdetails.JooqUserDetailsRepository", WEB_STARTERS)
                classFqName("com.drinkit.security.userdetails.InternalUserDetailsService", WEB_STARTERS)
                classFqName("com.drinkit.security.SecurityConfig", WEB_STARTERS)
                classFqName("com.drinkit.sse.SseApi", WEB_STARTERS)
                classFqName("com.drinkit.sse.InMemoryEmitters", WEB_STARTERS)
                classFqName("com.drinkit.webclient.rest.RestClientRetryConfiguration", WEB_STARTERS)
                classFqName("com.drinkit.mail.infra.LogEmailSender", MESSAGING_STARTERS)
                classFqName("com.drinkit.mail.infra.SpringEmailSender", MESSAGING_STARTERS)
                classFqName("com.drinkit.messaging.springevents.SpringEventsPublisherPlatform", MESSAGING_STARTERS)
                classFqName("com.drinkit.messaging.springevents.PlatformEventHandlerPostProcessor", MESSAGING_STARTERS)
                classFqName("com.drinkit.messaging.springevents.SpringEventsConfig", MESSAGING_STARTERS)
                classFqName("com.drinkit.featureflags.infra.FeatureFlagsConfig", OPS_STARTERS)
                classFqName("com.drinkit.featureflags.infra.OpenFeatureFeatureFlags", OPS_STARTERS)
                classFqName("com.drinkit.featureflags.infra.OpenFeatureHealthIndicator", OPS_STARTERS)
                classFqName("com.drinkit.ocr.InternalOCRAnalysis", OPS_STARTERS)
                classFqName("com.drinkit.ocr.OCRConfiguration", OPS_STARTERS)
            }
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
            suppress {
                function("com.drinkit.postgresql.jooq.JooqConfig.jooqConfigurationCustomizer", DATA_STARTERS)
                function("com.drinkit.search.engine.MeilisearchConfig.meilisearchClient", DATA_STARTERS)
                function("com.drinkit.security.SecurityConfig.authenticationManager", WEB_STARTERS)
                function("com.drinkit.security.SecurityConfig.sessionContextRepository", WEB_STARTERS)
                function("com.drinkit.security.SecurityConfig.securityContextHolderStrategy", WEB_STARTERS)
                function(
                    "com.drinkit.webclient.rest.RestClientRetryConfiguration.retryingHttpServiceGroupConfigurer",
                    WEB_STARTERS,
                )
                function(
                    "com.drinkit.messaging.springevents.SpringEventsConfig.applicationEventMulticaster",
                    MESSAGING_STARTERS,
                )
                function("com.drinkit.featureflags.infra.FeatureFlagsConfig.openFeatureAPI", OPS_STARTERS)
                function("com.drinkit.featureflags.infra.FeatureFlagsConfig.featureFlagFilter", OPS_STARTERS)
                function("com.drinkit.featureflags.infra.FeatureFlagsConfig.openFeatureFliptClient", OPS_STARTERS)
                function("com.drinkit.ocr.OCRConfiguration.createOCRAnalyzers", OPS_STARTERS)
            }
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
            suppress { classFqName("com.drinkit.search.engine.MeilisearchProperties", DATA_STARTERS) }
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
    fun `the package of a tech starter is named after the module`() {
        rule("starters.package-named-after-module") {
            description = "A tech starter's code, test fixtures included, lives in the package named after its " +
                "module, com.drinkit.event.sourcing for event-sourcing-starter: rename the package or the module"
            suppress {
                files(LIBRARY_STARTERS) { it.packageName == "com.drinkit.utils" }
                files(DATA_STARTERS) { it.packageName == "com.drinkit.jooq" }
            }
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
