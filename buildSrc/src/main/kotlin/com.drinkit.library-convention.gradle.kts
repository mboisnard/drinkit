plugins {
    id("com.drinkit.common-convention")
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    implementation(project(":documentation-starter"))
    implementation(project(":event-sourcing-starter"))
    implementation(project(":kotlin-starter"))
}