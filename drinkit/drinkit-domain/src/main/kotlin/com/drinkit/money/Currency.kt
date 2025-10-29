package com.drinkit.money

enum class Currency {
    EUR,
    USD,
    GBP,
    JPY,
    CNY;

    val code: String = this.name

    companion object {
        fun fromCode(code: String): Currency? = entries.find { it.code == code }
    }
}
