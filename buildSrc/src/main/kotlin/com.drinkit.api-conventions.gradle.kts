plugins {
    id("com.drinkit.common-conventions")

    id("com.gorylenko.gradle-git-properties")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

springBoot {
    buildInfo()
}

gitProperties {
    gitPropertiesName = "git.properties"
    keys = listOf("git.branch", "git.commit.id", "git.commit.time", "git.commit.message.short")
}