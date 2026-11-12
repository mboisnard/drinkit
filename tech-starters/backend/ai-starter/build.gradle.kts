plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("tools.jackson.module:jackson-module-kotlin")
    compileOnly(project(":rate-limit-starter"))
    compileOnly(project(":security-starter"))

    testImplementation(project(":rate-limit-starter"))
    testImplementation(testFixtures(project(":rate-limit-starter")))
    testImplementation(project(":security-starter"))
    testImplementation(testFixtures(project(":security-starter")))
}
