plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.openfeign:feign-okhttp")
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
}
