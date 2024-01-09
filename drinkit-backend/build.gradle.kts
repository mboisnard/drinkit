plugins {
    id("com.drinkit.api-conventions")
    //id("com.drinkit.contract-first")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))
}

/*
openApiGenerate {
    apiPackage.set("com.drinkit.api.generated.api")
    modelPackage.set("com.drinkit.api.generated.model")
}*/