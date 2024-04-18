plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.test-fixtures-conventions")
}

dependencies {
    implementation(libs.tess4j)
    implementation("com.google.cloud:spring-cloud-gcp-starter-vision")
}