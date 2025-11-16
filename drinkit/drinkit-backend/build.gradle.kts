plugins {
    id("com.drinkit.api-convention")
    id("com.drinkit.contract-first")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))

    implementation(project(":messaging-starter"))
    implementation(project(":ocr-starter"))
    implementation(project(":security-starter"))
    implementation(project(":sse-starter"))

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    openApiInput(project(":drinkit-api-contract", "openApi"))
}

openApiGenerate {
    apiPackage.set("com.drinkit.api.generated.api")
    modelPackage.set("com.drinkit.api.generated.model")

    typeMappings.set(
        typeMappings.get() + mapOf(
            "string+cellar-id" to "CellarId",
            "string+user-id" to "UserId",
        )
    )
    importMappings.set(
        importMappings.get() + mapOf(
            "CellarId" to "com.drinkit.cellar.core.CellarId",
            "UserId" to "com.drinkit.user.core.UserId",
        )
    )
}

// Simplify configuration for local application launch
// (use Spring dev profile and enforce project root folder to find local docker compose file)
tasks.bootRun {
    systemProperty("spring.profiles.active", findProperty("spring.profiles.active") ?: "dev")
    systemProperty("ROOT_FOLDER", "../../")
}