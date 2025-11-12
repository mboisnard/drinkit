plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
}

dependencies {
    api("org.springframework:spring-tx")
    api(project(":retryable-starter"))
}
