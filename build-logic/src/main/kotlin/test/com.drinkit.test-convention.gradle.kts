plugins {
    kotlin("jvm")
}

// The tests read the time in UTC, as CI does, whatever the zone of the machine
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("user.timezone", "UTC")
}

dependencies {
    testImplementation(project(":test-starter"))
}
