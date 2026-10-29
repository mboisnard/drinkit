// A Spring Boot application that runs once, then stops, such as the updater: no web server, no Actuator
plugins {
    id("com.drinkit.common-convention")

    id("org.springframework.boot")
}

dependencies {
    // Created by the Spring Boot plugin outside any source set, so the platform of common-convention misses it
    developmentOnly(platform(project(":platform")))
}

// Its image reaches every environment, so it resolves exactly the versions its lock lists, as api-convention does
dependencyLocking {
    lockAllConfigurations()
}
