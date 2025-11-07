package com.drinkit.money.forex.core

import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.money.Currency
import com.drinkit.utils.SAFE_ROUNDING_MODE
import java.math.BigDecimal

@CoreDomain
data class ExchangeRate private constructor(
    val source: Currency,
    val target: Currency,
    val value: BigDecimal,
) {
    init {
        require(value > BigDecimal.ZERO) { "Exchange rate must be positive" }
        require(source != target) { "Source and target currencies must be different" }
    }

    companion object {
        // Decimal precision commonly used for exchange rate values
        const val EXCHANGE_RATE_SCALE_PRECISION = 8

        fun from(source: Currency, target: Currency, value: BigDecimal) = ExchangeRate(
            source = source,
            target = target,
            value = value.setScale(EXCHANGE_RATE_SCALE_PRECISION, SAFE_ROUNDING_MODE)
        )
    }
}