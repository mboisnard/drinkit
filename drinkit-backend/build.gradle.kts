plugins {
    id("com.drinkit.api-conventions")
    id("com.drinkit.contract-first")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))

    openApiInput(project(":drinkit-api-contract", "openApi"))
}


openApiGenerate {
    apiPackage.set("com.drinkit.api.generated.api")
    modelPackage.set("com.drinkit.api.generated.model")
}