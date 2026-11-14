plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(project(":security-starter"))
    implementation(project(":rate-limit-starter"))
    implementation(libs.tika.core)

    // The JDK's HTTP client replaces Netty and Apache: the starter makes a few blocking calls
    implementation("software.amazon.awssdk:s3") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
        exclude(group = "software.amazon.awssdk", module = "apache5-client")
        exclude(group = "software.amazon.awssdk", module = "apache-client")
    }
    implementation("software.amazon.awssdk:url-connection-client")

    testFixturesImplementation("org.testcontainers:testcontainers")
    testFixturesImplementation("software.amazon.awssdk:s3")

    testImplementation(testFixtures(project(":security-starter")))
    testImplementation(testFixtures(project(":rate-limit-starter")))
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}

jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "stored_file"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.file.storage.generated.jooq"
                }
            }
        }
    }
}
