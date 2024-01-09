import gradle.kotlin.dsl.accessors._7555b15aff178c9149d1ac9f5a954af9.springBoot

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
    gitPropertiesName = "app-revision.properties"
    keys = listOf("git.branch", "git.commit.id", "git.commit.time", "git.commit.message.short")
}