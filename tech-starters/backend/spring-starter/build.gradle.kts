plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("tools.jackson.module:jackson-module-kotlin")
}
