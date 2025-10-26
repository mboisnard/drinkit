plugins {
    id("com.drinkit.common-convention")
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