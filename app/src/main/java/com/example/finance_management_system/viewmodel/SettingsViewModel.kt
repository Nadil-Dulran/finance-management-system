package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _statusState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferredCurrency,
        _statusState,
    ) { currency, status ->
        status.copy(preferredCurrency = currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun savePreferredCurrency(currency: String) {
        viewModelScope.launch {
            _statusState.update { it.copy(isSaving = true, message = null) }
            preferencesRepository.setPreferredCurrency(currency)
            _statusState.update { it.copy(isSaving = false, message = AppDefaults.SUCCESS_CURRENCY_UPDATED) }
        }
    }
}
