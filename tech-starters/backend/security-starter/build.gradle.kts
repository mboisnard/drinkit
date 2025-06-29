plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.jooq-codegen-conventions")
}

dependencies {
    api(project(":postgresql-starter"))

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.session:spring-session-jdbc")
    api(libs.jooq)
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "user"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.security.generated.jooq"
                }
            }
        }
    }
}