plugins {
    id("com.drinkit.api-conventions")
    id("com.drinkit.contract-first")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))

    implementation(project(":security-starter"))
    implementation("org.jooq:jooq:3.19.3") // Remove this dependency here (fix version using platform & constraints)
    implementation(project(":messaging-starter"))

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