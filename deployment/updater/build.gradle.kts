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

// Node runs Squawk, installed from package-lock.json without running any package script
val squawk by tasks.registering(Exec::class) {
    description = "Lints the SQL of the changelogs with Squawk, as .squawk.toml configures it"
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "cit", "--ignore-scripts", "--silent")
    inputs.files("package.json", "package-lock.json", ".squawk.toml")
    inputs.dir("src/main/resources/db/changelog")
    val passed = layout.buildDirectory.file("squawk/passed")
    outputs.file(passed)

    // Squawk has printed each finding with its rule, the SQL and a fix: say what to do next instead of an exit code
    isIgnoreExitValue = true
    val nextStep = "Squawk found unsafe SQL in the changelogs above. Each finding names its rule, links to its page " +
        "and shows a fix. The section \"Checked Rules\" of docs/src/engineering/guidelines/database.md says why " +
        "each rule matters. When a rule does not apply to a statement, write `-- squawk-ignore <rule>` on its own " +
        "line, right below a `-- <reason>` comment."
    doLast {
        if (executionResult.get().exitValue != 0) throw GradleException(nextStep)
        passed.get().asFile.writeText("")
    }
}

tasks.check {
    dependsOn(squawk)
}
