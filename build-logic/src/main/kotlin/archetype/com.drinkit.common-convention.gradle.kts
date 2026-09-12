import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library` // Expand the 'java' default plugin with dependencies api/implementation concepts https://docs.gradle.org/current/userguide/java_library_plugin.html

    kotlin("jvm")
    kotlin("plugin.spring") // Use allopen plugin to open Kotlin Spring Beans https://kotlinlang.org/docs/all-open-plugin.html
    id("idea")

    id("com.drinkit.code-analysis-conventions")
    id("com.drinkit.test-convention")
}

group = "com.drinkit"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

dependencies {
    // A platform only constrains the configuration it is declared in, or those extending it, so it
    // is declared on every source set: implementation, testImplementation, testFixturesImplementation.
    // compileClasspath and runtimeClasspath extend those, which is what makes `api` dependencies
    // constrained too. Configurations outside of any source set need it explicitly, see api-convention.
    sourceSets.all {
        implementationConfigurationName(platform(project(":platform")))
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)

        // Null safety management https://docs.spring.io/spring-boot/docs/3.0.13/reference/htmlsingle/#features.kotlin.null-safety
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
