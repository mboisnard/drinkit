plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    api("tools.jackson.module:jackson-module-kotlin") // Be able to use Jooq converter for JSON/JSONB columns
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-persistence") // Translates the exceptions of a @Repository
    api("org.springframework.boot:spring-boot-starter-jooq")
    api("org.postgresql:postgresql")
    api(libs.jooq)
    api(libs.jooq.kotlin) // mapping(::Type) of nested MULTISET rows

    testFixturesApi("org.testcontainers:testcontainers-postgresql")
}
