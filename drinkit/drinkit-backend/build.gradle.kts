plugins {
    id("com.drinkit.api-conventions")
    id("com.drinkit.contract-first")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))

    implementation(project(":security-starter"))
    implementation(project(":messaging-starter"))
    implementation(libs.jooq) // TODO Remove this dependency here (fix version using platform & constraints)

    //developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    //developmentOnly(files("../../deployment/local/docker-compose.yml"))

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
            "CellarId" to "com.drinkit.cellar.CellarId",
            "UserId" to "com.drinkit.user.UserId",
        )
    )
}