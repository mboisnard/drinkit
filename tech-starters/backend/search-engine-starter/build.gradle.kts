plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api(libs.meilisearch.java.client)
    compileOnly("org.springframework.boot:spring-boot-health")

    testImplementation("org.springframework.boot:spring-boot-health")
    testFixturesApi(libs.testcontainers.meilisearch)
}
