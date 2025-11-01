package com.drinkit.money.forex

import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.money.Currency
import com.drinkit.money.forex.core.ExchangeRate
import com.drinkit.money.forex.core.ExchangeRate.Companion.EXCHANGE_RATE_SCALE_PRECISION
import com.drinkit.money.forex.spi.ExchangeRates
import com.drinkit.utils.safelyDivide
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Usecase
class FindExchangeRate(
    private val exchangeRates: ExchangeRates,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun invoke(source: Currency, target: Currency): ExchangeRate? {
        require(source != target) {
            "Source and target currency must be different"
        }

       return exchangeRates.find(source, target)
           ?: crossRateCalculationFromEuro(source, target)
    }

    private fun crossRateCalculationFromEuro(source: Currency, target: Currency): ExchangeRate? {

        val euroToSource = exchangeRates.find(Currency.EUR, source) ?: run {
            logger.warn { "Missing exchange rate for EUR -> $source" }
            return null
        }

        if (target == Currency.EUR) {
            return ExchangeRate.from(
                source = source,
                target = Currency.EUR,
                value = BigDecimal.ONE.safelyDivide(euroToSource.value, EXCHANGE_RATE_SCALE_PRECISION)
            )
        }

        val euroToTarget = exchangeRates.find(Currency.EUR, target) ?: run {
            logger.warn { "Missing exchange rate for EUR -> $target" }
            return null
        }

        return ExchangeRate.from(
            source = source,
            target = target,
            value = euroToTarget.value.safelyDivide(euroToSource.value, EXCHANGE_RATE_SCALE_PRECISION)
        )
    }
}