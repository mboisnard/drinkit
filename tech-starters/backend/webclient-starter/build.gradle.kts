plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-restclient")

    implementation("tools.jackson.module:jackson-module-kotlin")
    api("tools.jackson.dataformat:jackson-dataformat-xml")
}
