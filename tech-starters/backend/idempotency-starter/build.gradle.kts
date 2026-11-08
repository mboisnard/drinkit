plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly(project(":security-starter"))

    testImplementation(project(":security-starter"))
    testImplementation(testFixtures(project(":security-starter")))
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "idempotency_key"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.idempotency.generated.jooq"
                }
            }
        }
    }
}
