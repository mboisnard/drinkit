// Add-on convention: java-test-fixtures pulls in java-library on its own, and the module-wide setup
// comes from the archetype this is combined with.
plugins {
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(project(":kotlin-starter"))
    testFixturesImplementation(project(":test-starter"))
}
