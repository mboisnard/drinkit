import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.kotlin.dsl.withType

// Add-on convention: only the Kotlin source sets and the KSP extension it configures below. The
// module-wide setup comes from the convention this is always combined with.
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

val documentationStarter = ":documentation-starter"
val isDocumentationStarter = project.path == documentationStarter

dependencies {
    if (isDocumentationStarter) {
        // The starter cannot depend on itself for its main code: its processor reads its test sources instead, so
        // that kspKotlin writes its page from its README
        "kspTest"(project(documentationStarter))
    } else {
        compileOnly(project(documentationStarter))
        ksp(project(documentationStarter))
    }
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

if (isDocumentationStarter) {
    // kspTestKotlin needs the main classes, which need kspKotlin: it can only run after it, never before
    tasks.withType<KspAATask>().named { it == "kspKotlin" }.configureEach {
        finalizedBy("kspTestKotlin")
    }

    // --rerun only reaches kspKotlin, and the processor writes the page outside the outputs of kspTestKotlin
    tasks.withType<KspAATask>().named { it == "kspTestKotlin" }.configureEach {
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    }
}
