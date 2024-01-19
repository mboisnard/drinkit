plugins {
    `kotlin-dsl` // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    // We need to implement plugin libraries here to be able to use it in `plugins` section in conventions
    // Use directly alias in plugin section when it will work
    implementation(pluginLibs.all.open.plugin)
    implementation(pluginLibs.gradle.git.properties.plugin)
    implementation(pluginLibs.gradle.kotlin.plugin)
    implementation(pluginLibs.jooq.codegen.gradle.plugin)
    implementation(pluginLibs.openapi.generator.plugin)
    implementation(pluginLibs.spring.dependency.management.plugin)
    implementation(pluginLibs.spring.boot.gradle.plugin)
}