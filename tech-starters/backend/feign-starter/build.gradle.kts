plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
}
