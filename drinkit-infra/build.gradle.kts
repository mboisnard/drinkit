plugins {
    id("com.drinkit.library-conventions")

    id("org.jooq.jooq-codegen-gradle")
}

dependencies {
    implementation(project(":drinkit-domain"))
    implementation(libs.bson)
}

jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"

            target {
                packageName = "com.drinkit.generated.jooq"
            }
        }
    }
}