plugins {
    id("com.drinkit.library-conventions")

    id("org.jooq.jooq-codegen-gradle")
}

dependencies {
    implementation(project(":drinkit-domain"))

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation(libs.jooq)
    implementation(libs.bson)

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
                includes = "cellar"
                inputSchema = "public"
            }
            target {
                packageName = "com.drinkit.generated.jooq"
                directory = "src/generated/jooq/kotlin"
            }
        }
    }
}