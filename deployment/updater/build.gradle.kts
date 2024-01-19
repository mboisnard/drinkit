plugins {
    id("com.drinkit.common-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")
}