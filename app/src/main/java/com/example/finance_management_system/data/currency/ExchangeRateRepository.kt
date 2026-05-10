package com.example.finance_management_system.data.currency

import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ExchangeRateRepository(
    private val preferencesRepository: UserPreferencesRepository,
) {
    val ratesToLkr: Flow<Map<String, Double>> = preferencesRepository.cachedRates.map { cached ->
        fallbackRates() + cached
    }

    suspend fun refreshRatesIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val lastUpdated = preferencesRepository.getLastRatesUpdatedAt()
        val twelveHours = 12 * 60 * 60 * 1000L

        if (!force && now - lastUpdated < twelveHours) return

        val liveRates = fetchLatestRates()
        if (liveRates.isNotEmpty()) {
            preferencesRepository.cacheRates(liveRates, now)
        }
    }

    suspend fun getRateToLkr(currency: String): Double {
        val code = currency.uppercase()
        val cached = preferencesRepository.getCachedRates()
        return (fallbackRates() + cached)[code] ?: 1.0
    }

    private suspend fun fetchLatestRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        runCatching {
            val supportedFiat = listOf("USD", "EUR", "GBP", "AED", "SGD", "AUD", "JPY", "INR")
            val endpoint =
                "https://api.frankfurter.dev/v1/latest?base=LKR&symbols=${supportedFiat.joinToString(",")}"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val rates = json.getJSONObject("rates")

            buildMap {
                supportedFiat.forEach { code ->
                    if (rates.has(code)) {
                        val quotePerLkr = rates.getDouble(code)
                        if (quotePerLkr > 0.0) {
                            put(code, 1.0 / quotePerLkr)
                        }
                    }
                }
                put("LKR", 1.0)
            }
        }.getOrElse { emptyMap() }
    }

    private fun fallbackRates(): Map<String, Double> = mapOf(
        "LKR" to 1.0,
        "USD" to 300.0,
        "EUR" to 325.0,
        "GBP" to 380.0,
        "AED" to 82.0,
        "SGD" to 222.0,
        "AUD" to 196.0,
        "JPY" to 2.0,
        "INR" to 3.6,
        "USDT" to 300.0,
        "ETH" to 950000.0,
    )
}
