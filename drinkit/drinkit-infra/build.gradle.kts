plugins {
    id("com.drinkit.library-conventions")

    id("org.jooq.jooq-codegen-gradle")
}

dependencies {
    implementation(project(":drinkit-domain"))

    // Database dependencies
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("org.jooq:jooq:3.19.2") //Fix version with plaform module with constraints
    implementation(libs.bson)

    jooqCodegen("org.postgresql:postgresql")

    // Spring Security dependencies
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.security:spring-security-crypto")
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
                includes = "cellar | user | role"
                inputSchema = "public"
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