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
    implementation(pluginLibs.all.open.plugin)
    implementation(pluginLibs.detekt.plugin)
    implementation(pluginLibs.graalvm.buildtools.native.plugin)
    implementation(pluginLibs.gradle.git.properties.plugin)
    implementation(pluginLibs.gradle.kotlin.plugin)
    implementation(pluginLibs.jooq.codegen.gradle.plugin)
    implementation(pluginLibs.jooq.meta)
    implementation(pluginLibs.openapi.generator.plugin)
    implementation(pluginLibs.spring.dependency.management.plugin)
    implementation(pluginLibs.spring.boot.gradle.plugin)
}
