plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
}

dependencies {
    api("org.springframework:spring-context")
    api("org.springframework.retry:spring-retry")

    implementation(project(":kotlin-starter"))
}
