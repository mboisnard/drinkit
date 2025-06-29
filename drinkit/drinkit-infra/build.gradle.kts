plugins {
    id("com.drinkit.library-conventions")
    id("com.drinkit.jooq-codegen-conventions")
}

dependencies {
    implementation(project(":drinkit-domain"))

    // Database dependencies
    implementation(project(":postgresql-starter"))
    implementation(libs.jooq) // TODO remove after adding platform version constraint
    implementation(libs.bson)

    implementation(project(":mail-starter"))

    testImplementation(testFixtures(project(":drinkit-domain")))
    testImplementation(testFixtures(project(":postgresql-starter")))
}

jooqCodegenConfig {
    tables = "cellar | user | role | verification_token"
    schema = "drinkit_application"
    packageName = "com.drinkit.generated.jooq"
}