plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    implementation(project(":messaging-starter"))
    compileOnly(project(":security-starter"))

    testImplementation(project(":security-starter"))
    testImplementation(testFixtures(project(":security-starter")))
    testImplementation(testFixtures(project(":messaging-starter")))
    testImplementation(testFixtures(project(":postgresql-starter")))
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}
