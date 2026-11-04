plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.documentation-convention")
}

dependencies {
    api(libs.kotlin.logging.jvm)
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}
