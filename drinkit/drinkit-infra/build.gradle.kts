plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation(project(":drinkit-domain"))

    implementation(libs.bson)

    implementation(project(":configuration-starter"))
    implementation(project(":event-sourcing-starter"))
    implementation(project(":i18n-starter"))
    implementation(project(":mail-starter"))
    implementation(project(":http-client-starter"))

    testImplementation(testFixtures(project(":drinkit-domain")))
    testImplementation(testFixtures(project(":i18n-starter")))
    testImplementation(testFixtures(project(":mail-starter")))
    testImplementation(testFixtures(project(":messaging-starter")))
}

// The catalog the frontend shares, which i18n-starter reads from the classpath
tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("translations")) { into("translations") }
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "cellar | user_event | user | exchange_rate"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.generated.jooq"
                }
            }
        }
    }
}
