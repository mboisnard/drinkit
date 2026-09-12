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
    // postgresql-starter contain all dependencies needed to generate jooq classes (postgresql driver with BOM managed version)
    jooqCodegen(project(":postgresql-starter"))

    implementation(project(":postgresql-starter"))
    testImplementation(testFixtures(project(":postgresql-starter")))
}


/**
 * Jooq executions element will generate gradle tasks that you can override in any module using this convention
 *
 * jooq {
 *     executions.getByName("main") {
 *         configuration.apply {
 *             generator.apply {
 *                 database.apply {
 *                     includes = "table names"
 *                     inputSchema = "schema name"
 *                 }
 *                 target.apply {
 *                     packageName = "..."
 *                 }
 *             }
 *         }
 *     }
 * }
 */
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
                        isKotlinNotNullPojoAttributes = true // Generate non-nullable types on POJO attributes, where column is not null
                        isKotlinNotNullRecordAttributes = true // Generate non-nullable types on Record attributes, where column is not null
                        isKotlinNotNullInterfaceAttributes = true // Generate non-nullable types on interface attributes, where column is not null
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
