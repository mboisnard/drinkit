import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.FailOnSeverity
import dev.detekt.gradle.report.ReportMergeTask

plugins {
    id("dev.detekt")
}

// What detekt analyses depends on where this convention lands, and that single fact drives
// everything below. On a module: the three tasks that resolve types — the plain `detekt` task and
// the per-source-set ones do not, so any rule needing a compiled classpath silently never fires.
// On the root project, which has no source set: the plain `detekt` task, pointed at the Gradle
// scripts, which belong to no source set and would otherwise never be analysed at all.
val analysesGradleScripts = project == rootProject
val enabledDetektTasks =
    if (analysesGradleScripts) setOf("detekt") else setOf("detektMain", "detektTest", "detektTestFixtures")

dependencies {
    // Formatting rules, versioned off the tool so they cannot drift from the engine running them
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:${detekt.toolVersion.get()}")
}

detekt {
    parallel = true
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/code-analysis/detekt/detekt.yml")

    // One file for the whole repository, next to detekt.yml. Every project reads it; only the
    // `detektBaseline*` tasks are per-source-set, and regenerating from scratch is not the way this
    // file is meant to change — you delete a line when you fix what it holds.
    baseline = file("$rootDir/code-analysis/detekt/baseline.xml")

    // Repository-relative paths in the SARIF report, which is how GitHub maps a finding to a file
    basePath = rootProject.layout.projectDirectory

    // Any finding fails the build; detekt's own default is to fail on Error alone
    failOnSeverity = FailOnSeverity.Info

    // `--auto-correct` is a per-task option and would bind to a single task on a multi-task command
    // line; this property applies to all of them.
    autoCorrect = providers.gradleProperty("detekt.autoCorrect").map(String::toBoolean).orElse(false)

    if (analysesGradleScripts) {
        source.setFrom(
            fileTree(rootDir) {
                include("**/*.gradle.kts")
                exclude("**/build/**")
            },
        )
    }
}

// One merged report: GitHub takes only a handful of SARIF files per category.
// https://detekt.dev/docs/introduction/reporting/#merging-reports
val detektReportMergeSarif =
    rootProject.tasks.maybeCreate("detektReportMergeSarif", ReportMergeTask::class.java).apply {
        output.convention(rootProject.layout.buildDirectory.file("reports/detekt/detekt-all-projects.sarif"))
    }

// Generated code: JOOQ under src/generated, OpenAPI under build/. Matched on the absolute path,
// Ant patterns here being resolved against each source root rather than the project directory.
fun PatternFilterable.excludeGeneratedCode() = exclude {
    it.file.path.contains("/src/generated/") || it.file.path.contains("/build/")
}

tasks.withType<Detekt>().configureEach {
    // Leaving the others on would analyse everything twice and duplicate every merged finding
    enabled = name in enabledDetektTasks

    // Auto-correction rewrites the very files Gradle snapshotted as inputs, so the snapshot it
    // records is the *unformatted* state. The next run on that same unformatted content is judged
    // up to date and the correction silently never happens. Only the correcting runs skip the
    // check; the analysis the CI runs stays incremental.
    outputs.upToDateWhen { !autoCorrect.get() }

    excludeGeneratedCode()

    // All four formats are on by default; only SARIF and HTML are wanted
    reports {
        checkstyle.required = false
        markdown.required = false
    }

    // Only enabled tasks feed the merge, or the disabled ones' stale reports are merged in too
    if (isEnabled) {
        finalizedBy(detektReportMergeSarif)
        detektReportMergeSarif.input.from(reports.sarif.outputLocation)
    }
}

val detektAll = tasks.register("detektAll") {
    group = "verification"
    description = "Runs every enabled detekt task of this project."
    dependsOn(tasks.withType<Detekt>().matching { it.name in enabledDetektTasks })
}

// `matching` rather than `named`: the root project has no `check` task to hook into
tasks.matching { it.name == "check" }.configureEach {
    dependsOn(detektAll)
}

// DetektCreateBaselineTask does not extend Detekt, so it inherits none of the above. Without this
// it records the generated code too — 1117 entries instead of 82.
tasks.withType<DetektCreateBaselineTask>().configureEach {
    excludeGeneratedCode()
}
