plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.test-fixtures-conventions")
}

dependencies {
    implementation(project(":messaging-starter"))
    testFixturesApi(testFixtures(project(":messaging-starter")))
}