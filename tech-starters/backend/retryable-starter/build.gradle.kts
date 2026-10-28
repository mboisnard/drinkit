plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":kotlin-starter"))

    testFixturesApi("org.springframework:spring-context")
}
