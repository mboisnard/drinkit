plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "rate_limit"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.rate.limit.generated.jooq"
                }
            }
        }
    }
}
