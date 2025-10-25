import org.jooq.codegen.gradle.CodegenTask

plugins {
    kotlin("jvm")
    id("org.jooq.jooq-codegen-gradle")
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.7.8") //TODO check why I need the version here

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
                        url = "jdbc:postgresql://localhost:5432/drinkit"
                        username = "drinkit"
                        password = "admin"
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
                        directory = "src/generated/jooq/kotlin"
                    }

                    strategy {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(tasks.jooqCodegen)
        }
    }
}

// Execute the JooqCodegen task only when you want (avoid having automatic jooq generation on build task)
tasks.withType<CodegenTask> {
    onlyIf { gradle.startParameter.taskNames.contains("jooqCodegen") }
}
