package com.drinkit.money

import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.money.forex.core.ExchangeRate
import com.drinkit.utils.SAFE_ROUNDING_MODE
import com.drinkit.utils.addIfNotMatch
import java.math.BigDecimal

@CoreDomain
data class Money private constructor(
    val amount: BigDecimal,
    val currency: Currency,
) {
    companion object {
        // Cents as scale
        const val MONEY_SCALE_PRECISION = 2

        fun from(amount: BigDecimal, currency: Currency) = Money(
            amount = amount.setScale(MONEY_SCALE_PRECISION, SAFE_ROUNDING_MODE),
            currency = currency,
        )
    }

    fun validate() = buildList {
        addIfNotMatch(amount >= BigDecimal.ZERO, "Amount must be positive or zero")
    }

    fun convertWith(exchangeRate: ExchangeRate): Money {
        require(exchangeRate.source == currency) {
            "ExchangeRate source currency must be equals to Money currency"
        }

        return from(
            amount = amount * exchangeRate.value,
            currency = exchangeRate.target,
        )
    }
}
