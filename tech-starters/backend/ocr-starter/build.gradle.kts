plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation(libs.tess4j)
    implementation("com.google.cloud:spring-cloud-gcp-starter-vision")
}