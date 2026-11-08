package com.drinkit.idempotency.infra.jooq

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.generated.jooq.DrinkitApplication
import com.drinkit.idempotency.infra.IdempotencyTestContract
import com.drinkit.idempotency.infra.KeyedIdempotency
import com.drinkit.postgresql.JooqIntegrationTest
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqIdempotencyTest : IdempotencyTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchIdempotency(): Idempotency = KeyedIdempotency(JooqIdempotencyKeys(dsl), clock, properties)
}
