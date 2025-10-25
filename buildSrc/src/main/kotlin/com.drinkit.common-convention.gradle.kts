import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library` // Expand the 'java' default plugin with dependencies api/implementation concepts https://docs.gradle.org/current/userguide/java_library_plugin.html

    kotlin("jvm")
    kotlin("plugin.spring") // Use allopen plugin to open Kotlin Spring Beans https://kotlinlang.org/docs/all-open-plugin.html
    id("idea")

    id("io.spring.dependency-management")
    id("com.drinkit.code-analysis-conventions")
    id("com.drinkit.test-convention")
}

group = "com.drinkit"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_22
}

dependencies {
    sourceSets.all {
        // implementationConfigurationName replaces api/runtimeOnly/testImplementation(platform(project(":platform"))
        implementationConfigurationName(platform(project(":platform")))
    }
}

kotlin {
    jvmToolchain(24)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_22)

        // Null safety management https://docs.spring.io/spring-boot/docs/3.0.13/reference/htmlsingle/#features.kotlin.null-safety
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}