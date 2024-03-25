import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

plugins {
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
}

detekt {
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    parallel = true
    buildUponDefaultConfig = true
    //ignoreFailures = true
    //autoCorrect = true
    config.setFrom("$rootDir/code-analysis/detekt/detekt.yml")
    baseline = file("$rootDir/code-analysis/detekt/baseline.xml")
    basePath = rootProject.projectDir.absolutePath
}


// Create a dedicate task to merge all reports from each gradle project
// https://detekt.dev/docs/introduction/reporting/#merging-reports
val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/detekt-all-projects.sarif"))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "1.8"
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
    finalizedBy(detektReportMergeSarif)
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"
}

detektReportMergeSarif {
    input.from(tasks.withType<Detekt>().map { it.sarifReportFile })
}