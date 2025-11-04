plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation(project(":drinkit-domain"))

    implementation(libs.bson)

    implementation(project(":configuration-starter"))
    implementation(project(":mail-starter"))
    implementation(project(":webclient-starter"))

    testImplementation(testFixtures(project(":drinkit-domain")))
    testImplementation(testFixtures(project(":messaging-starter")))
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "cellar | user_event | user | verification_token | exchange_rate"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.generated.jooq"
                }
            }
        }
    }
}