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
}
