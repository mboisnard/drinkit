plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    implementation(project(":messaging-starter"))
}
