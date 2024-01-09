plugins {
    id("com.drinkit.api-conventions")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(project(":drinkit-infra"))
}