plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.test-fixtures-conventions")
}

dependencies {
    implementation(project(":event-sourcing-starter"))
    implementation(project(":messaging-starter"))

    testFixturesImplementation(testFixtures(project(":messaging-starter")))
}