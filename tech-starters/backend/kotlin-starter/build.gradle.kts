plugins {
    id("com.drinkit.common-convention")
}

dependencies {
    api(libs.kotlin.logging.jvm)
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}
