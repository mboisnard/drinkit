plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation(project(":configuration-starter"))
    implementation("org.springframework.session:spring-session-jdbc")
    api("org.springframework.boot:spring-boot-starter-security")

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
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