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

jooqCodegenConfig {
    tables = "user"
    schema = "drinkit_application"
    packageName = "com.drinkit.security.generated.jooq"
}