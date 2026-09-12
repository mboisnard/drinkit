plugins {
    `kotlin-dsl` // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
}

// Conventions are grouped in subfolders of src/main/kotlin for readability only: a plugin id comes
// from the file name alone, never from its folder. Do NOT add a `package` declaration to them —
// that would prefix every id with the package name and break every `id(...)` in the build.
// File names must stay unique across all folders, since the id is what identifies them.

// The Java version used to build the convention plugins themselves. It comes from the same catalog
// entry as the one the conventions apply to the application, so both stay in sync
kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
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
    implementation(pluginLibs.ksp.plugin)
    implementation(pluginLibs.openapi.generator.plugin)
    implementation(pluginLibs.spring.boot.gradle.plugin)
}
