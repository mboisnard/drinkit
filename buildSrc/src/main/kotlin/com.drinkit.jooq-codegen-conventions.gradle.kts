import org.jooq.codegen.gradle.CodegenTask

plugins {
    kotlin("jvm")
    id("org.jooq.jooq-codegen-gradle")
}

// Declare an extension to configure jOOQ code generation inside other gradle modules
abstract class JooqCodegenExtension {
    lateinit var tables: String
    lateinit var schema: String
    lateinit var packageName: String
}
val jooqCodegenConfig = extensions.create<JooqCodegenExtension>("jooqCodegenConfig")

dependencies {
    jooqCodegen("org.postgresql:postgresql")
}

// Configure jOOQ after the project has been evaluated to allow for customization
afterEvaluate {
    jooq {
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
                    includes = jooqCodegenConfig.tables
                    inputSchema = jooqCodegenConfig.schema
                }
                generate {
                    isKotlinNotNullPojoAttributes = true // Generate non-nullable types on POJO attributes, where column is not null
                    isKotlinNotNullRecordAttributes = true // Generate non-nullable types on Record attributes, where column is not null
                    isKotlinNotNullInterfaceAttributes = true // Generate non-nullable types on interface attributes, where column is not null
                    isPojosAsKotlinDataClasses = true
                }
                target {
                    packageName = jooqCodegenConfig.packageName
                    directory = "src/generated/jooq/kotlin"
                }

                strategy {
                    name = "org.jooq.codegen.DefaultGeneratorStrategy"
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

tasks.withType<CodegenTask> {
    onlyIf { gradle.startParameter.taskNames.contains("jooqCodegen") }
}
