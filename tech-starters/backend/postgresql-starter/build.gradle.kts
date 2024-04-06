plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.test-fixtures-conventions")
}

dependencies {
    api("com.fasterxml.jackson.module:jackson-module-kotlin") // Be able to use Jooq converter for JSON/JSONB columns
    api("org.springframework.boot:spring-boot-starter-jooq")
    api("org.postgresql:postgresql")
    api(libs.jooq)

    testFixturesApi("org.testcontainers:postgresql")
}