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
    // Cucumber's modules ask each other for version ranges: its BOM pins them to the ones it was released with
    api(platform(libs.cucumber.dependencies.bom))
    api(platform(libs.spring.ai.dependencies.bom))
    api(platform(libs.spring.boot.dependencies.bom))
    api(platform(libs.spring.cloud.gcp.dependencies.bom))

    constraints {
        // Runtime Dependencies
        // The three BouncyCastle modules check each other's version: they move together
        api(libs.bouncycastle.bcpkix)
        api(libs.bouncycastle.bcprov)
        api(libs.bouncycastle.bcutil)
        api(libs.bson)
        api(libs.jooq)
        api(libs.jooq.kotlin)
        api(libs.kotlin.logging.jvm)
        api(libs.meilisearch.java.client)
        api(libs.springdoc.openapi.starter.webmvc.ui)
        api(libs.tess4j)

        // Test Dependencies
        api(libs.approvaltests)
        api(libs.rest.assured)
        api(libs.konture)
        api(libs.kotest.assertions.core)
        api(libs.kotlin.faker)
        api(libs.testcontainers.meilisearch)
    }
}
