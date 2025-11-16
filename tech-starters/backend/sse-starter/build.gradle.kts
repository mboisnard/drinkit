plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":messaging-starter"))
}