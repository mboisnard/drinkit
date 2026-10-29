import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.drinkit.job-convention")
    id("com.drinkit.squawk-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.postgresql:postgresql")
}

// The image reads the changelogs from /workspace, after the jar on the classpath: a copy in the jar would shadow
// them. The local datasource of the dev profile stays out too, so a run without credentials fails
tasks.bootJar {
    archiveFileName = "updater.jar"
    exclude("db/**", "application-dev.yml")
}

tasks.bootRun {
    systemProperty("spring.profiles.active", findProperty("spring.profiles.active") ?: "dev")
}

// The layers of Spring Boot that the Dockerfile copies: extracted here, the image build runs no code of the jar
tasks.register<JavaExec>("extractLayers") {
    val jar = tasks.bootJar.flatMap { it.archiveFile }
    val destination = layout.buildDirectory.dir("image")
    outputs.dir(destination)
    classpath(jar)
    mainClass = "org.springframework.boot.loader.launch.JarLauncher"
    systemProperty("jarmode", "tools")
    argumentProviders.add {
        listOf("extract", "--layers", "--destination", destination.get().asFile.absolutePath)
    }
    // A jar a bump replaced would otherwise stay among the layers
    doFirst { destination.get().asFile.deleteRecursively() }
}

// A broken changelog rule prints its id and its fix in the console, not only in the test report
tasks.test {
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
    }
}
