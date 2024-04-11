import org.jooq.codegen.gradle.CodegenTask
import org.jooq.meta.jaxb.*

plugins {
    kotlin("jvm")
    id("org.jooq.jooq-codegen-gradle")
}

dependencies {
    jooqCodegen("org.postgresql:postgresql")
}

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
                includes = "cellar | user | role | verification_token"
                inputSchema = "drinkit_application"
            }
            generate {
                isKotlinNotNullPojoAttributes = true // Generate non-nullable types on POJO attributes, where column is not null
                isKotlinNotNullRecordAttributes = true // Generate non-nullable types on Record attributes, where column is not null
                isKotlinNotNullInterfaceAttributes = true // Generate non-nullable types on interface attributes, where column is not null
                isPojosAsKotlinDataClasses = true
            }
            target {
                packageName = "com.drinkit.generated.jooq"
                directory = "src/generated/jooq/kotlin"
            }

            strategy {
                name = "org.jooq.codegen.DefaultGeneratorStrategy"
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
