plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-restclient")

    implementation("tools.jackson.module:jackson-module-kotlin")
    api("tools.jackson.dataformat:jackson-dataformat-xml")
}
