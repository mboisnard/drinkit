plugins {
    id("com.drinkit.common-conventions")
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    testImplementation(project(":test-starter"))
}