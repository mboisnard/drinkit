package com.drinkit.jooq

import org.jooq.Schema
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.reflect.KClass

@ExtendWith(JooqPostgresExtension::class)
annotation class JooqIntegrationTest(
    val schemas: Array<KClass<out Schema>> = [],
)
