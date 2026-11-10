package com.drinkit.rate.limit.infra.gcra.jooq

import com.drinkit.postgresql.toStoredTime
import com.drinkit.rate.limit.generated.jooq.tables.references.RATE_LIMIT
import com.drinkit.rate.limit.infra.gcra.Outcome
import com.drinkit.rate.limit.infra.gcra.TheoreticalArrivals
import org.jooq.DSLContext
import org.jooq.impl.DSL.excluded
import org.jooq.impl.DSL.row
import org.jooq.impl.DSL.select
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private const val FORGOTTEN_PER_UPDATE = 100

// Counted apart from the caller's transaction: a rollback of the call must not give its slot back
@Transactional(propagation = Propagation.REQUIRES_NEW)
internal class JooqTheoreticalArrivals(private val dsl: DSLContext) : TheoreticalArrivals {

    override fun update(
        rule: String,
        keyHash: String,
        now: Instant,
        decide: (Map<Duration, Instant>) -> Outcome,
    ): Outcome {
        forget(before = now)

        // A key's first call has no row to lock yet: the advisory lock serializes it too
        dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", "$rule:$keyHash")

        val arrivals = dsl.selectFrom(RATE_LIMIT)
            .where(RATE_LIMIT.RULE.eq(rule).and(RATE_LIMIT.KEY_HASH.eq(keyHash)))
            .fetch()
            .associate { Duration.parse(it.period) to it.theoreticalArrival.toInstant() }

        return decide(arrivals).also { if (it is Outcome.Allowed) save(rule, keyHash, it.theoreticalArrivals) }
    }

    private fun save(rule: String, keyHash: String, arrivals: Map<Duration, Instant>) {
        val rows = arrivals.map { (period, arrival) -> row(rule, keyHash, period.toString(), arrival.toStoredTime()) }

        dsl.insertInto(
            RATE_LIMIT,
            RATE_LIMIT.RULE,
            RATE_LIMIT.KEY_HASH,
            RATE_LIMIT.PERIOD,
            RATE_LIMIT.THEORETICAL_ARRIVAL,
        )
            .valuesOfRows(rows)
            .onConflict(RATE_LIMIT.RULE, RATE_LIMIT.KEY_HASH, RATE_LIMIT.PERIOD)
            .doUpdate()
            .set(RATE_LIMIT.THEORETICAL_ARRIVAL, excluded(RATE_LIMIT.THEORETICAL_ARRIVAL))
            .execute()
    }

    // A passed arrival counts as none. PostgreSQL has no DELETE … LIMIT, and SKIP LOCKED leaves the rows another
    // call is changing, so that two cleanups never wait on each other
    private fun forget(before: Instant) {
        dsl.deleteFrom(RATE_LIMIT)
            .where(
                row(RATE_LIMIT.RULE, RATE_LIMIT.KEY_HASH, RATE_LIMIT.PERIOD).`in`(
                    select(RATE_LIMIT.RULE, RATE_LIMIT.KEY_HASH, RATE_LIMIT.PERIOD)
                        .from(RATE_LIMIT)
                        .where(RATE_LIMIT.THEORETICAL_ARRIVAL.lt(before.toStoredTime()))
                        .limit(FORGOTTEN_PER_UPDATE)
                        .forUpdate()
                        .skipLocked(),
                ),
            )
            .execute()
    }
}
