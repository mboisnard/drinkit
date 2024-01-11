plugins {
    id("com.drinkit.library-conventions")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(libs.bson)
}