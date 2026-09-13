plugins {
    // Adds the api/implementation separation on top of the `java` plugin
    // https://docs.gradle.org/current/userguide/java_library_plugin.html
    `java-library`

    kotlin("jvm")
    // Opens Spring beans, which Kotlin makes final by default
    // https://kotlinlang.org/docs/all-open-plugin.html
    kotlin("plugin.spring")
    id("idea")

    id("com.drinkit.code-analysis-convention")
    id("com.drinkit.test-convention")
}

group = "com.drinkit"
version = "0.0.1-SNAPSHOT"

// Explicit lookup here: Precompiled script plugins get no generated `libs` accessor
val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // A platform only constrains the configuration it is declared in, or those extending it, so it
    // is declared on every source set: implementation, testImplementation, testFixturesImplementation.
    // compileClasspath and runtimeClasspath extend those, which is what makes `api` dependencies
    // constrained too. Configurations outside of any source set need it explicitly, see api-convention.
    sourceSets.all {
        implementationConfigurationName(platform(project(":platform")))
    }
}

// The toolchain is the single source of truth for the Java version: the Kotlin plugin derives its
// own jvmTarget from it, and java.sourceCompatibility becomes redundant
kotlin {
    jvmToolchain(libs.findVersion("java").get().requiredVersion.toInt())

    compilerOptions {
        // Null safety management https://docs.spring.io/spring-boot/reference/features/kotlin.html
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
