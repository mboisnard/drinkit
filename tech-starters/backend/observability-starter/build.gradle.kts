plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // The JDK sender, not OkHttp's: OkHttp 5 would replace the OkHttp 4 that the Meilisearch client is built on
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry") {
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }
    implementation("io.opentelemetry:opentelemetry-exporter-sender-jdk")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}
