plugins {
    id("com.drinkit.common-conventions")
    id("org.springframework.boot")

    id("com.gorylenko.gradle-git-properties")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(project(":kotlin-starter"))

    testImplementation(project(":test-starter"))
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