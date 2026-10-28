plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
}

dependencies {
    api("org.springframework:spring-context")
    api("org.springframework:spring-tx")

    runtimeOnly(project(":retryable-starter"))
}
