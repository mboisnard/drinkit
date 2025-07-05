plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation(project(":event-sourcing-starter"))
    implementation(project(":messaging-starter"))

    testFixturesImplementation(testFixtures(project(":messaging-starter")))
}