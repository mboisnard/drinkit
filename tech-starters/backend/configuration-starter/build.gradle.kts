plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-cache")
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "configuration"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.configuration.generated.jooq"
                }
            }
        }
    }
}
