plugins {
    id("com.drinkit.library-conventions")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.postgresql:postgresql")
    api("org.jooq:jooq:3.19.2")
}