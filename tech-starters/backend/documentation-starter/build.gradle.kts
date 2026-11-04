import com.google.devtools.ksp.gradle.KspAATask

plugins {
    id("com.drinkit.common-convention")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)

    // The starter cannot apply documentation-convention, which would make its main code depend on itself: its
    // processor reads its test sources instead, so that `kspKotlin` writes its page from its README
    kspTest(project(":documentation-starter"))
}

ksp {
    arg("docsOutputDir", rootProject.file("docs/src/engineering/resources").absolutePath)
    arg("moduleName", project.name)
    arg("moduleSourceDir", projectDir.absolutePath)
}

val kspExplicitlyRequested = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "kspKotlin" }

tasks.withType<KspAATask>().configureEach {
    enabled = kspExplicitlyRequested
}

// kspTestKotlin needs the main classes, which need kspKotlin: it can only run after it, never before
tasks.withType<KspAATask>().named { it == "kspKotlin" }.configureEach {
    finalizedBy("kspTestKotlin")
}

// --rerun only reaches kspKotlin, and the processor writes the page outside the outputs of kspTestKotlin
tasks.withType<KspAATask>().named { it == "kspTestKotlin" }.configureEach {
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}
