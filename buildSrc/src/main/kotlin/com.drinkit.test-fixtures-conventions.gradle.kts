plugins {
    id("com.drinkit.common-conventions")
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(project(":test-starter"))
}
