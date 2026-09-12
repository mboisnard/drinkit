plugins {
    kotlin("jvm")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    testImplementation(project(":test-starter"))
}