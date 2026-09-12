plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    implementation(project(":event-sourcing-starter"))
    implementation(project(":kotlin-starter"))
}