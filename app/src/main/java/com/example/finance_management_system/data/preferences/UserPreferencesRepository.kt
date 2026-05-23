package com.example.finance_management_system.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AuthSessionState(
    val isLoggedIn: Boolean,
    val lastLoginTime: Long,
    val userLoggedOut: Boolean,
)

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preferredCurrency: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_CURRENCY] ?: "LKR"
    }

    val onboardingCompleted: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val cachedRates: Flow<Map<String, Double>> = context.userPreferencesDataStore.data.map { prefs ->
        decodeRates(prefs[KEY_CACHED_RATES])
    }

    val isLoggedIn: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    val lastLoginTime: Flow<Long> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_LAST_LOGIN_TIME] ?: 0L
    }

    val userLoggedOut: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_USER_LOGGED_OUT] ?: true
    }

    suspend fun setPreferredCurrency(currency: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_PREFERRED_CURRENCY] = currency
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun cacheRates(rates: Map<String, Double>, updatedAt: Long) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_CACHED_RATES] = encodeRates(rates)
            prefs[KEY_RATES_UPDATED_AT] = updatedAt
        }
    }

    suspend fun getCachedRates(): Map<String, Double> {
        return context.userPreferencesDataStore.data.map { prefs ->
            decodeRates(prefs[KEY_CACHED_RATES])
        }.first()
    }

    suspend fun getLastRatesUpdatedAt(): Long {
        return context.userPreferencesDataStore.data.map { prefs ->
            prefs[KEY_RATES_UPDATED_AT] ?: 0L
        }.first()
    }

    suspend fun getAuthSessionState(): AuthSessionState {
        return context.userPreferencesDataStore.data.map { prefs ->
            AuthSessionState(
                isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false,
                lastLoginTime = prefs[KEY_LAST_LOGIN_TIME] ?: 0L,
                userLoggedOut = prefs[KEY_USER_LOGGED_OUT] ?: true,
            )
        }.first()
    }

    suspend fun saveAuthSession(lastLoginTime: Long = System.currentTimeMillis()) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_LAST_LOGIN_TIME] = lastLoginTime
            prefs[KEY_USER_LOGGED_OUT] = false
        }
    }

    suspend fun clearAuthSession() {
        context.userPreferencesDataStore.edit { prefs ->
            val loggedOut = prefs[KEY_USER_LOGGED_OUT] ?: true
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_LAST_LOGIN_TIME] = 0L
            prefs[KEY_USER_LOGGED_OUT] = loggedOut
        }
    }

    suspend fun markUserLoggedOut() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_LAST_LOGIN_TIME] = 0L
            prefs[KEY_USER_LOGGED_OUT] = true
        }
    }

    private companion object {
        val KEY_PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_CACHED_RATES = stringPreferencesKey("cached_rates")
        val KEY_RATES_UPDATED_AT = longPreferencesKey("rates_updated_at")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_LAST_LOGIN_TIME = longPreferencesKey("last_login_time")
        val KEY_USER_LOGGED_OUT = booleanPreferencesKey("user_logged_out")

        fun encodeRates(rates: Map<String, Double>): String {
            return rates.entries.joinToString(";") { "${it.key}=${it.value}" }
        }

        fun decodeRates(raw: String?): Map<String, Double> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(";")
                .mapNotNull { pair ->
                    val parts = pair.split("=")
                    if (parts.size != 2) return@mapNotNull null
                    val code = parts[0]
                    val rate = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                    code to rate
                }
                .toMap()
        }
    }
}
