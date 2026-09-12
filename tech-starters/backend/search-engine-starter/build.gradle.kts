plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    // Typed configuration properties
    implementation("org.springframework.boot:spring-boot")

    api(libs.meilisearch.java.client)

    testFixturesApi(libs.testcontainers.meilisearch)
}