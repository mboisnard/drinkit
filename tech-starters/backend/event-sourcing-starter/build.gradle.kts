plugins {
    id("com.drinkit.common-convention")
}

dependencies {
    api("org.springframework:spring-tx")
    api(project(":retryable-starter"))
}
