package com.drinkit.jooq

import org.jooq.Table
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.reflect.KClass

@ExtendWith(JooqPostgresExtension::class)
annotation class JooqIntegrationTest(
    val tables: Array<KClass<out Table<*>>> = [],
)
