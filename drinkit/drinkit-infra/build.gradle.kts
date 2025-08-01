plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation(project(":drinkit-domain"))

    // Database dependencies
    implementation(project(":postgresql-starter"))
    implementation(libs.jooq) // TODO remove after adding platform version constraint
    implementation(libs.bson)

    implementation(project(":mail-starter"))

    testImplementation(testFixtures(project(":drinkit-domain")))
    testImplementation(testFixtures(project(":messaging-starter")))
    testImplementation(testFixtures(project(":postgresql-starter")))
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "cellar | user_event | user | verification_token"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.generated.jooq"
                }
            }
        }
    }
}