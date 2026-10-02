plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation(project(":event-sourcing-starter"))
    implementation(project(":messaging-starter"))
    implementation("org.apache.commons:commons-text:1.9")

    testImplementation(testFixtures(project(":messaging-starter")))
    testFixturesImplementation(project(":event-sourcing-starter"))
    testFixturesImplementation(testFixtures(project(":messaging-starter")))
}
