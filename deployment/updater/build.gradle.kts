import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.drinkit.common-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.postgresql:postgresql")
}

// A broken changelog rule prints its id and its fix in the console, not only in the test report
tasks.test {
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
    }
}

// Node installs Squawk from package-lock.json, without running any package script. npm prints its own errors
val installSquawk = tasks.register<Exec>("installSquawk") {
    description = "Installs Squawk from package-lock.json"
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "ci", "--ignore-scripts", "--no-audit", "--no-fund", "--loglevel=error")
    inputs.files("package.json", "package-lock.json")
    outputs.dir("node_modules")
}

val squawk = tasks.register<Exec>("squawk") {
    description = "Lints the SQL of the changelogs with Squawk, as .squawk.toml configures it"
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "test", "--silent")
    inputs.files(installSquawk)
    inputs.file(".squawk.toml")
    inputs.dir("src/main/resources/db/changelog")
    val passed = layout.buildDirectory.file("squawk/passed")
    outputs.file(passed)

    // Squawk has printed each finding with its rule, the SQL and a fix: say what to do next instead of an exit code
    isIgnoreExitValue = true
    val nextStep = "Squawk failed on the changelogs, as printed above. A finding names its rule, links to its page " +
        "and shows a fix, and a configuration error names its line in .squawk.toml. The section \"Checked Rules\" " +
        "of docs/src/engineering/guidelines/database.md says why each rule matters. When a rule does not apply to " +
        "a statement, write `-- squawk-ignore <rule>` on its own line, right below a `-- <reason>` comment."
    doLast {
        if (executionResult.get().exitValue != 0) throw GradleException(nextStep)
        passed.get().asFile.writeText("")
    }
}

tasks.check {
    dependsOn(squawk)
}
