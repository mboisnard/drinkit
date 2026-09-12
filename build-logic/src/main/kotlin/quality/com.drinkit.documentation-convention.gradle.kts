import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.kotlin.dsl.withType

// Add-on convention: only the Kotlin source sets and the KSP extension it configures below. The
// module-wide setup comes from the convention this is always combined with.
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":documentation-starter"))
    ksp(project(":documentation-starter"))
}

ksp {
    arg("docsOutputDir", project.rootProject.file("docs/src/engineering/resources").absolutePath)
    arg("moduleName", project.name)
    arg("moduleSourceDir", project.projectDir.absolutePath)
}

// Documentation is only generated when kspKotlin is explicitly asked for, never as part of `build`.
// The flag is resolved at configuration time so the check stays configuration-cache compatible, and
// comparing on the last path segment also matches a qualified `:some-module:kspKotlin` request
val kspExplicitlyRequested = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "kspKotlin" }

tasks.withType<KspAATask>().configureEach {
    enabled = kspExplicitlyRequested
}