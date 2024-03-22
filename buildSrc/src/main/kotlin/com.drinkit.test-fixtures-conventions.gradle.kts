plugins {
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(project(":test-starter"))
}
