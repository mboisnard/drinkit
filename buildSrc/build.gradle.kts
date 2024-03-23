plugins {
    `kotlin-dsl` // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // We need to implement plugin libraries here to be able to use it in `plugins` section in conventions
    // https://docs.gradle.org/current/userguide/custom_plugins.html#applying_external_plugins_in_precompiled_script_plugins
    implementation(libs.all.open.plugin)
    implementation(libs.detekt.plugin)
    implementation(libs.graalvm.buildtools.native.plugin)
    implementation(libs.gradle.git.properties.plugin)
    implementation(libs.gradle.kotlin.plugin)
    implementation(libs.jooq.codegen.gradle.plugin)
    implementation(libs.jooq.meta)
    implementation(libs.openapi.generator.plugin)
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.spring.boot.gradle.plugin)
}