plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(libs.openfeature.sdk)
    implementation(libs.openfeature.flipt.provider)
    api(project(":security-starter"))
    compileOnly("org.springframework.boot:spring-boot-health")

    testImplementation("org.springframework.boot:spring-boot-health")
    testImplementation(testFixtures(project(":security-starter")))
}
