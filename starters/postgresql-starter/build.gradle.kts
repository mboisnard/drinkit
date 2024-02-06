plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.test-fixtures-conventions")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-jooq")
    api("org.postgresql:postgresql")
    api(libs.jooq)

    testFixturesApi("org.testcontainers:postgresql")
}