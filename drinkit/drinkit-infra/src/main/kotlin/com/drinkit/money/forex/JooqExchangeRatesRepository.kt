package com.drinkit.money.forex

import com.drinkit.money.Currency
import com.drinkit.money.forex.spi.ExchangeRates
import com.drinkit.generated.jooq.tables.ExchangeRate.Companion.EXCHANGE_RATE
import com.drinkit.generated.jooq.tables.records.ExchangeRateRecord
import com.drinkit.money.forex.core.ExchangeRate
import com.drinkit.postgresql.jooq.JooqRepository
import org.jooq.DSLContext
import java.time.Clock
import java.time.OffsetDateTime

@JooqRepository
internal class JooqExchangeRatesRepository(
    private val dsl: DSLContext,
    private val clock: Clock,
) : ExchangeRates {

    override fun find(source: Currency, target: Currency): ExchangeRate? {
        val query = dsl.selectFrom(EXCHANGE_RATE)
            .where(EXCHANGE_RATE.SOURCE_CURRENCY.eq(source.code))
            .and(EXCHANGE_RATE.TARGET_CURRENCY.eq(target.code))

        return query.fetchOne { it.toDomain() }
    }

    override fun saveOrUpdate(exchangeRates: Set<ExchangeRate>): Set<ExchangeRate> {
        if (exchangeRates.isEmpty()) {
            return emptySet()
        }

        val now = OffsetDateTime.now(clock)
        val queries = exchangeRates.map {
            dsl.insertInto(EXCHANGE_RATE)
                .set(EXCHANGE_RATE.SOURCE_CURRENCY, it.source.code)
                .set(EXCHANGE_RATE.TARGET_CURRENCY, it.target.code)
                .set(EXCHANGE_RATE.RATE, it.value)
                .set(EXCHANGE_RATE.MODIFIED, now)
                .onConflict(EXCHANGE_RATE.SOURCE_CURRENCY, EXCHANGE_RATE.TARGET_CURRENCY)
                .doUpdate()
                .set(EXCHANGE_RATE.RATE, it.value)
                .set(EXCHANGE_RATE.MODIFIED, now)
        }

        dsl.batch(queries).execute()

        return exchangeRates
    }

    private fun ExchangeRateRecord.toDomain(): ExchangeRate {
        val sourceCurrency = Currency.fromCode(sourceCurrency)
            ?: error("Unknown source currency: $sourceCurrency")
        val targetCurrency = Currency.fromCode(targetCurrency)
            ?: error("Unknown target currency: $targetCurrency")

        return ExchangeRate.from(
            source = sourceCurrency,
            target = targetCurrency,
            value = rate,
        )
    }
}
