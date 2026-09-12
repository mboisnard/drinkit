plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")

    id("org.springframework.boot")

    id("com.gorylenko.gradle-git-properties")
    id("org.graalvm.buildtools.native")
}

dependencies {
    // `developmentOnly` is created by the Spring Boot plugin and belongs to no source set, so the
    // platform declared in common-convention never reaches it: a platform only constrains the
    // configuration it is declared in, or those extending it
    developmentOnly(platform(project(":platform")))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(project(":kotlin-starter"))
    implementation(project(":monitoring-starter"))
}

// Create a `build-info.properties` file in resource folder, info available in Actuator
springBoot {
    buildInfo()
}

// Create a `git.properties` file in resource folder, info available in Actuator
gitProperties {
    gitPropertiesName = "git.properties"
    keys = listOf("git.branch", "git.commit.id", "git.commit.time", "git.commit.message.short")
}