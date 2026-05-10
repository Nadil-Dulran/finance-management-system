package com.example.finance_management_system.data.currency

import java.util.Locale

object CurrencyConverter {
    fun toLkr(amount: Double, currency: String, rateToLkr: Double): Double {
        return amount * rateToLkr
    }

    fun fromLkr(amountLkr: Double, rateToLkr: Double): Double {
        val rate = rateToLkr
        return if (rate == 0.0) amountLkr else amountLkr / rate
    }

    fun format(amount: Double, currency: String): String {
        val digits = when (currency.uppercase()) {
            "ETH" -> 4
            else -> 2
        }
        return "%s %,.${digits}f".format(Locale.US, currency.uppercase(), amount)
    }
}
