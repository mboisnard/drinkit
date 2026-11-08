plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation(project(":messaging-starter"))

    testFixturesApi("org.springframework.boot:spring-boot-starter-mail")
    testFixturesImplementation("org.testcontainers:testcontainers")

    testImplementation(testFixtures(project(":messaging-starter")))
    testImplementation(project(":idempotency-starter"))
    testImplementation(testFixtures(project(":idempotency-starter")))
}
