// Add-on convention: it only declares what it technically needs
// (the Kotlin source sets and dependency configurations it touches below)
plugins {
    kotlin("jvm")
    id("org.jooq.jooq-codegen-gradle")
}

// Generated sources are committed to the repository, not produced on every build
val generatedSourcesDir = "src/generated/jooq/kotlin"

// Defaults match deployment/local/compose.yml; override with -PjooqJdbcUrl=... to generate against
// another database without editing the convention
val jdbcUrl = providers.gradleProperty("jooqJdbcUrl")
    .getOrElse("jdbc:postgresql://localhost:5432/drinkit")
val jdbcUser = providers.gradleProperty("jooqJdbcUser")
    .getOrElse("drinkit")
val jdbcPassword = providers.gradleProperty("jooqJdbcPassword")
    .getOrElse("admin")

dependencies {
    // Code generation specific dependencies
    // postgresql-starter carries the driver jooq codegen needs, at the BOM-managed version
    jooqCodegen(project(":postgresql-starter"))

    implementation(project(":postgresql-starter"))
    testImplementation(testFixtures(project(":postgresql-starter")))
}

// Each jooq execution registers a gradle task that a module applying this convention can override:
//
// jooq {
//     executions.getByName("main") {
//         configuration.apply {
//             generator.apply {
//                 database.apply {
//                     includes = "table names"
//                     inputSchema = "schema name"
//                 }
//                 target.apply {
//                     packageName = "..."
//                 }
//             }
//         }
//     }
// }
jooq {
    executions {
        create("main") {
            configuration {
                generator {
                    name = "org.jooq.codegen.KotlinGenerator"

                    jdbc {
                        driver = "org.postgresql.Driver"
                        url = jdbcUrl
                        username = jdbcUser
                        password = jdbcPassword
                    }
                    database {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                    }
                    generate {
                        // Non-nullable Kotlin types wherever the column is NOT NULL
                        isKotlinNotNullPojoAttributes = true
                        isKotlinNotNullRecordAttributes = true
                        isKotlinNotNullInterfaceAttributes = true
                        isPojosAsKotlinDataClasses = true
                    }
                    target {
                        directory = generatedSourcesDir
                    }

                    strategy {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

// Generated sources are committed, so the source set points at the directory rather than at the
// task: compiling never triggers a regeneration, and `jooqCodegen` stays an explicit, on-demand task
kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generatedSourcesDir)
        }
    }
}
