package com.drinkit.idempotency.infra.jooq

import com.drinkit.idempotency.generated.jooq.tables.records.IdempotencyKeyRecord
import com.drinkit.idempotency.generated.jooq.tables.references.IDEMPOTENCY_KEY
import com.drinkit.idempotency.infra.HeldKey
import com.drinkit.idempotency.infra.IdempotencyKeys
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL.row
import org.jooq.impl.DSL.select
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

private const val FORGOTTEN_PER_TAKE = 100

// Taken and freed apart from the caller's transaction: another instance must see the key before the action ends
@Transactional(propagation = Propagation.REQUIRES_NEW)
internal class JooqIdempotencyKeys(private val dsl: DSLContext) : IdempotencyKeys {

    override fun take(key: HeldKey, forgetBefore: Instant): HeldKey {
        forget(before = forgetBefore)

        dsl.insertInto(IDEMPOTENCY_KEY)
            .set(IDEMPOTENCY_KEY.SCOPE, key.scope)
            .set(IDEMPOTENCY_KEY.KEY_HASH, key.hash)
            .set(IDEMPOTENCY_KEY.OWNER, key.owner)
            .set(IDEMPOTENCY_KEY.FINGERPRINT, key.fingerprint)
            .set(IDEMPOTENCY_KEY.TAKEN_AT, key.takenAt.atUtc())
            .set(IDEMPOTENCY_KEY.LEASE_UNTIL, key.leaseUntil.atUtc())
            .onConflict(IDEMPOTENCY_KEY.SCOPE, IDEMPOTENCY_KEY.KEY_HASH)
            .doUpdate()
            .set(IDEMPOTENCY_KEY.OWNER, key.owner)
            .set(IDEMPOTENCY_KEY.FINGERPRINT, key.fingerprint)
            .set(IDEMPOTENCY_KEY.TAKEN_AT, key.takenAt.atUtc())
            .set(IDEMPOTENCY_KEY.LEASE_UNTIL, key.leaseUntil.atUtc())
            .setNull(IDEMPOTENCY_KEY.COMPLETED_AT)
            .setNull(IDEMPOTENCY_KEY.OUTCOME)
            .where(
                IDEMPOTENCY_KEY.TAKEN_AT.lt(forgetBefore.atUtc())
                    .or(
                        IDEMPOTENCY_KEY.COMPLETED_AT.isNull
                            .and(IDEMPOTENCY_KEY.LEASE_UNTIL.lt(key.takenAt.atUtc()))
                            .and(IDEMPOTENCY_KEY.FINGERPRINT.eq(key.fingerprint)),
                    ),
            )
            .execute()

        return dsl.selectFrom(IDEMPOTENCY_KEY).where(sameKey(key)).fetchSingle().toHeldKey()
    }

    // Joins the transaction open around the action, if any, so that its outcome commits with its writes
    @Transactional(propagation = Propagation.REQUIRED)
    override fun complete(key: HeldKey, outcome: ByteArray?, at: Instant) {
        val completed = dsl.update(IDEMPOTENCY_KEY)
            .set(IDEMPOTENCY_KEY.COMPLETED_AT, at.atUtc())
            .set(IDEMPOTENCY_KEY.OUTCOME, outcome)
            .where(sameKey(key).and(IDEMPOTENCY_KEY.OWNER.eq(key.owner)))
            .execute()

        check(completed == 1) { "Another run took over the key once the lease of this one ended" }
    }

    override fun release(key: HeldKey) {
        dsl.deleteFrom(IDEMPOTENCY_KEY)
            .where(sameKey(key).and(IDEMPOTENCY_KEY.OWNER.eq(key.owner)))
            .execute()
    }

    // PostgreSQL has no DELETE … LIMIT: a bounded batch keeps each take short
    private fun forget(before: Instant) {
        dsl.deleteFrom(IDEMPOTENCY_KEY)
            .where(
                row(IDEMPOTENCY_KEY.SCOPE, IDEMPOTENCY_KEY.KEY_HASH).`in`(
                    select(IDEMPOTENCY_KEY.SCOPE, IDEMPOTENCY_KEY.KEY_HASH)
                        .from(IDEMPOTENCY_KEY)
                        .where(IDEMPOTENCY_KEY.TAKEN_AT.lt(before.atUtc()))
                        .limit(FORGOTTEN_PER_TAKE),
                ),
            )
            .execute()
    }

    private fun sameKey(key: HeldKey): Condition =
        IDEMPOTENCY_KEY.SCOPE.eq(key.scope).and(IDEMPOTENCY_KEY.KEY_HASH.eq(key.hash))

    private fun IdempotencyKeyRecord.toHeldKey() = HeldKey(
        scope = scope,
        hash = keyHash,
        owner = owner,
        fingerprint = fingerprint,
        takenAt = takenAt.toInstant(),
        leaseUntil = leaseUntil.toInstant(),
        completedAt = completedAt?.toInstant(),
        outcome = outcome,
    )

    private fun Instant.atUtc(): OffsetDateTime = atOffset(UTC)
}
