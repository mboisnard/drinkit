plugins {
    kotlin("jvm")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    testImplementation(project(":test-starter"))
}
