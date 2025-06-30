plugins {
    id("com.drinkit.common-conventions")
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    implementation(project(":documentation-starter"))
    implementation(project(":kotlin-starter"))

    testImplementation(project(":test-starter"))
}