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
}

// Execute the kspKotlin task only when you want (avoid having automatic documentation generation on build task)
tasks.withType<KspAATask> {
    onlyIf { gradle.startParameter.taskNames.contains("kspKotlin") }
}