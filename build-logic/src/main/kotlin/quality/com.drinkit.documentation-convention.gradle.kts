import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.kotlin.dsl.withType

plugins {
    id("com.drinkit.common-convention")
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
    // `enabled` rather than `onlyIf`: an onlyIf lambda holds a reference to this script, which the
    // configuration cache cannot serialize
    enabled = kspExplicitlyRequested
}