plugins {
    id("com.drinkit.common-no-dep-convention")
}

dependencies {
    api(libs.kotlin.logging.jvm)
    api("org.jetbrains.kotlin:kotlin-reflect")
}
