plugins {
    id("com.drinkit.api-convention")
    id("com.drinkit.contract-first-convention")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))

    implementation(project(":messaging-starter"))
    implementation(project(":ocr-starter"))
    implementation(project(":security-starter"))
    implementation(project(":sse-starter"))
    implementation(project(":feature-flags-starter"))

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    openApiInput(project(":drinkit-api-contract"))
}

openApiGenerate {
    apiPackage.set("com.drinkit.api.generated.api")
    modelPackage.set("com.drinkit.api.generated.model")

    typeMappings.putAll(
        mapOf(
            "string+cellar-id" to "CellarId",
            "string+user-id" to "UserId",
        ),
    )
    importMappings.putAll(
        mapOf(
            "CellarId" to "com.drinkit.cellar.core.CellarId",
            "UserId" to "com.drinkit.user.core.UserId",
        ),
    )
}

// Simplify configuration for local application launch
// (use Spring dev profile and enforce project root folder to find local docker compose file)
tasks.bootRun {
    systemProperty("spring.profiles.active", findProperty("spring.profiles.active") ?: "dev")
    systemProperty("ROOT_FOLDER", "../../")
}
