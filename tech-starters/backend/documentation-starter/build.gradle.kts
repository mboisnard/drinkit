plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
}
