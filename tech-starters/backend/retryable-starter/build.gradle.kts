plugins {
    id("com.drinkit.common-no-dep-convention")
}

dependencies {
    api("org.springframework:spring-context")
    api("org.springframework.retry:spring-retry")

    implementation(project(":kotlin-starter"))
}
