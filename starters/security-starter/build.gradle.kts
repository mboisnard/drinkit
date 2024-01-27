import org.jooq.codegen.gradle.CodegenTask

plugins {
    id("com.drinkit.library-conventions")
    id("org.jooq.jooq-codegen-gradle")
}

dependencies {
    api(project(":postgresql-starter"))

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.session:spring-session-jdbc")
    api("org.jooq:jooq:3.19.2")

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
                includes = "user | role"
                inputSchema = "public"
            }
            generate {
                isKotlinNotNullPojoAttributes = true // Generate non-nullable types on POJO attributes, where column is not null
                isKotlinNotNullRecordAttributes = true // Generate non-nullable types on Record attributes, where column is not null
                isKotlinNotNullInterfaceAttributes = true // Generate non-nullable types on interface attributes, where column is not null
                isPojosAsKotlinDataClasses = true
            }
            target {
                packageName = "com.drinkit.security.generated.jooq"
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