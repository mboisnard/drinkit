plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    api(libs.meilisearch.java.client)

    testFixturesApi(libs.testcontainers.meilisearch)
}