plugins {
    id("com.drinkit.common-no-dep-convention")
}

dependencies {
    implementation(project(":kotlin-starter"))

    api(libs.approvaltests)

    api(libs.cucumber.java)
    api(libs.cucumber.junit)
    api(libs.cucumber.spring)

    api(libs.kotest.assertions.core)
    api(libs.kotlin.faker)
    api(libs.io.rest.assured)

    api("org.springframework.boot:spring-boot-starter-test")
    api("org.jetbrains.kotlin:kotlin-test-junit5")
    runtimeOnly("org.junit.platform:junit-platform-launcher")

    api("org.springframework.boot:spring-boot-testcontainers")
    api("org.testcontainers:junit-jupiter")
}