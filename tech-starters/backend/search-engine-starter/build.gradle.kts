plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.test-fixtures-conventions")
}

dependencies {
    api(libs.meilisearch.java.client)

    testFixturesApi(libs.testcontainers.meilisearch)
}