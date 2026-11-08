package com.drinkit.mail.infra

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.inMemoryIdempotency
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean

// The auto-configurations come from the AutoConfiguration.imports files, as in an application. No PostgreSQL here:
// the keys of the idempotent delivery stay in memory
@SpringBootConfiguration
@EnableAutoConfiguration(
    excludeName = [
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "com.drinkit.postgresql.infra.PostgresqlAutoConfiguration",
    ],
)
internal class MailApplication {

    @Bean
    fun idempotency(): Idempotency = inMemoryIdempotency()
}
