plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-session-jdbc")
    api("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":rate-limit-starter"))

    compileOnly("org.springframework.boot:spring-boot-starter-actuator")

    testFixturesApi("org.springframework.security:spring-security-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testFixturesImplementation(project(":rate-limit-starter"))

    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation(testFixtures(project(":rate-limit-starter")))
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("tools.jackson.module:jackson-module-kotlin")
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "spring_session|spring_session_attributes|remember_me_token|sign_in_code"
                    inputSchema = "drinkit_session"
                }
                target.apply {
                    packageName = "com.drinkit.security.generated.jooq"
                }
            }
        }
    }
}
