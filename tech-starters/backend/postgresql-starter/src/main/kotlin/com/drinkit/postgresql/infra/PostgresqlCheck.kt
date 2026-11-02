package com.drinkit.postgresql.infra

import org.springframework.beans.factory.InitializingBean

internal class PostgresqlCheck(private val databaseProductName: () -> String) : InitializingBean {

    override fun afterPropertiesSet() {
        val productName = databaseProductName()
        check(productName == "PostgreSQL") {
            "The PostgreSQL starter needs a PostgreSQL database, but spring.datasource.url leads to $productName"
        }
    }
}
