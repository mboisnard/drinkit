package com.drinkit.architecture

import io.github.baole.konture.SourceSets
import io.github.baole.konture.rule
import org.junit.jupiter.api.Test

internal class TechStartersArchitectureTest {

    @Test
    fun `a tech starter depends on no application`() {
        rule("starters.depend-on-no-application") {
            description = "A tech starter holds one technical concern, reusable without the business code: replace " +
                "the dependency on an application module with a port the application implements"
            modules {
                that().haveNamePath(STARTERS)
                should().notDependOn(APPLICATIONS)
            }
        }.check()
    }

    @Test
    fun `the package of a tech starter is named after the module`() {
        rule("starters.package-named-after-module") {
            description = "A tech starter is found by its name: its code, test fixtures included, lives in the " +
                "package named after the module without -starter, its hyphens dropped or turned into dots, under " +
                "the root package: com.drinkit.event.sourcing for event-sourcing-starter. Rename the package or the " +
                "module"
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

    @Test
    fun `no package of a tech starter names a bounded context`() {
        rule("starters.serve-no-bounded-context") {
            description = "A tech starter holds technical code any application may use, so none of its packages " +
                "names a bounded context: move the business code into the application"
            val contextNames = contexts.filterNot { it.endsWith(".common") }.map { it.substringAfterLast('.') }
            files(SourceSets.production() or SourceSets.tests()) {
                that().inModules(STARTERS)
                should().satisfy { file, violations ->
                    file.declaration.packageName.split('.')
                        .filter { it in contextNames }
                        .forEach { violations += "${file.modulePath} ${file.declaration.name} names the context $it" }
                }
            }
        }.check()
    }

    @Test
    fun `only the public API of a tech starter is public, and it says so`() {
        rule("starters.public-api-is-marked") {
            description = "A tech starter shows only its public API, marked @TechStarterTool so that its " +
                "documentation lists it, and keeps the rest internal: mark the class, or make it internal"
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
            description = "A tech starter never makes a bean by annotating its class, which only a scan of the " +
                "starter's packages finds: declare the bean in the starter's @AutoConfiguration"
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
                classFqName("com.drinkit.sse.InMemoryEmittersRepository", WEB_STARTERS)
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
            description = "A bean of a tech starter gives way to the application's own: annotate it " +
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
            description = "The settings of a tech starter live under starters.<name>, so they never clash with " +
                "Spring's or the application's: change the prefix"
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
}
