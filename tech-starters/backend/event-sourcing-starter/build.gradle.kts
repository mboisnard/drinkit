plugins {
    id("com.drinkit.common-no-dep-convention")
}

dependencies {
    api("org.springframework:spring-tx")
    api(project(":retryable-starter"))

    testImplementation(project(":test-starter"))
}
