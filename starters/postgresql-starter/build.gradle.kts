plugins {
    id("com.drinkit.library-conventions")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.postgresql:postgresql")
    api(libs.jooq)
}