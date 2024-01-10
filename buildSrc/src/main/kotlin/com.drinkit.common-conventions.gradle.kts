import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library` // Expand the 'java' default plugin with dependencies api/implementation concepts https://docs.gradle.org/current/userguide/java_library_plugin.html

    kotlin("jvm")
    kotlin("plugin.spring") // Use allopen plugin to open Kotlin Spring Beans https://kotlinlang.org/docs/all-open-plugin.html
    id("idea")

    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.drinkit"
version = "0.0.1-SNAPSHOT"

// Here we can't use the version catalog directly because of this issue: https://github.com/gradle/gradle/issues/15383
dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}