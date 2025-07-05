plugins {
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(project(":kotlin-starter"))
    testFixturesImplementation(project(":test-starter"))
}
