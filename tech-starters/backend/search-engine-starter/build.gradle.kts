plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":messaging-starter"))
    api(libs.meilisearch.java.client)
    compileOnly("org.springframework.boot:spring-boot-health")

    testImplementation("org.springframework.boot:spring-boot-health")
    testFixturesApi(libs.testcontainers.meilisearch)
    testFixturesApi("tools.jackson.module:jackson-module-kotlin")
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "search_index_rebuild"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.search.engine.generated.jooq"
                }
            }
        }
    }
}
