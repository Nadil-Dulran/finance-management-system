package com.example.finance_management_system.data.currency

import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import com.example.finance_management_system.model.supportedCurrencies
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
    private val trackedCurrencies = supportedCurrencies.map { it.uppercase() }.distinct()

    val ratesToLkr: Flow<Map<String, Double>> = preferencesRepository.cachedRates.map { cached ->
        cached + mapOf("LKR" to 1.0)
    }

    suspend fun refreshRatesIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val lastUpdated = preferencesRepository.getLastRatesUpdatedAt()
        val oneDay = 24 * 60 * 60 * 1000L

        if (!force && now - lastUpdated < oneDay) return

        val liveRates = fetchLatestRates()
        if (liveRates.isNotEmpty()) {
            preferencesRepository.cacheRates(liveRates, now)
        }
    }

    suspend fun getRateToLkr(currency: String): Double {
        val code = currency.uppercase()
        if (code == "LKR") return 1.0

        refreshRatesIfNeeded()
        val cached = preferencesRepository.getCachedRates()
        return cached[code]
            ?: error("Exchange rate for $code is unavailable right now. Connect to the internet and try again.")
    }

    private suspend fun fetchLatestRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        runCatching {
            buildMap {
                put("LKR", 1.0)
                trackedCurrencies
                    .filterNot { it == "LKR" }
                    .forEach { code ->
                        fetchRateToLkr(code)?.let { put(code, it) }
                    }
            }
        }.getOrElse { emptyMap() }
    }

    private fun fetchRateToLkr(currencyCode: String): Double? {
        val endpoint = "https://api.coinbase.com/v2/exchange-rates?currency=$currencyCode"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")

        return connection.inputStream.bufferedReader().use { reader ->
            val json = JSONObject(reader.readText())
            val rates = json.getJSONObject("data").getJSONObject("rates")
            rates.optString("LKR")
                .toDoubleOrNull()
                ?.takeIf { it > 0.0 }
        }
    }
}
