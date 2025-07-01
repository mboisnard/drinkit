plugins {
    id("com.drinkit.common-conventions")
    id("org.springframework.boot")

    id("com.gorylenko.gradle-git-properties")
    id("org.graalvm.buildtools.native")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(project(":documentation-starter"))
    implementation(project(":kotlin-starter"))
    implementation(project(":monitoring-starter"))

    testImplementation(project(":test-starter"))
}

// Create a `build-info.properties` file in resource folder, info available in Actuator
springBoot {
    buildInfo()
}

// Create a `git.properties` file in resource folder, info available in Actuator
gitProperties {
    dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git") // Workaround for issue https://github.com/n0mer/gradle-git-properties/issues/240
    gitPropertiesName = "git.properties"
    keys = listOf("git.branch", "git.commit.id", "git.commit.time", "git.commit.message.short")
}