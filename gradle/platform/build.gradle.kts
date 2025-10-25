plugins {
    `java-platform`
}

// This platform defines the internal BOM for the project
// It imports third-party BOMs (Spring Boot, Spring Cloud GCP) and overrides
// specific dependency versions using our version catalog
// https://docs.gradle.org/current/userguide/java_platform_plugin.html
javaPlatform {
    allowDependencies()
}

dependencies {
    // Import third-party BOMs, our constraints below will override their versions
    api(platform(libs.spring.boot.dependencies.bom))
    api(platform(libs.spring.cloud.gcp.dependencies.bom))

    constraints {
        // Runtime Dependencies
        api(libs.bson)
        api(libs.jooq)
        api(libs.kotlin.logging.jvm)
        api(libs.meilisearch.java.client)
        api(libs.springdoc.openapi.starter.webmvc.ui)
        api(libs.tess4j)

        // Test Dependencies
        api(libs.approvaltests)
        api(libs.cucumber.java)
        api(libs.cucumber.junit)
        api(libs.cucumber.spring)
        api(libs.io.rest.assured)
        api(libs.kotest.assertions.core)
        api(libs.kotlin.faker)
        api(libs.testcontainers.meilisearch)
    }
}