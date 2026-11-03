plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-cache") // @Cacheable and @CacheEvict for the consumers
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework:spring-tx") // the transaction that holds the puts and evictions back

    testFixturesApi("com.github.ben-manes.caffeine:caffeine") // the Ticker a test advances
}
