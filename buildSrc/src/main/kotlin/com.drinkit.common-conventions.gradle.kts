import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library` // Expand the 'java' default plugin with dependencies api/implementation concepts https://docs.gradle.org/current/userguide/java_library_plugin.html

    kotlin("jvm")
    kotlin("plugin.spring") // Use allopen plugin to open Kotlin Spring Beans https://kotlinlang.org/docs/all-open-plugin.html
    id("idea")

    id("io.spring.dependency-management")
    id("com.drinkit.code-analysis-conventions")
    id("com.drinkit.test-conventions")
}

// Use the Spring Dependency Management BOM without importing the spring-boot plugin
// We don't want to use the spring-boot plugin in this common convention used ether by libraries and applications
// https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/#managing-dependencies.dependency-management-plugin.using-in-isolation
the<DependencyManagementExtension>().apply {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:5.10.0")
    }
}

group = "com.drinkit"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_23
}

kotlin {
    jvmToolchain(23)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)

        // Null safety management https://docs.spring.io/spring-boot/docs/3.0.13/reference/htmlsingle/#features.kotlin.null-safety
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}