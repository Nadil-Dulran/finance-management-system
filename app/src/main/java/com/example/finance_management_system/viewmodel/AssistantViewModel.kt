package com.tetrova_flowledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tetrova_flowledger.assistant.AssistantConversationMemory
import com.tetrova_flowledger.assistant.AssistantEngine
import com.tetrova_flowledger.assistant.AssistantFinanceSnapshot
import com.tetrova_flowledger.assistant.AssistantMessage
import com.tetrova_flowledger.assistant.AssistantMessageRole
import com.tetrova_flowledger.assistant.AssistantUiState
import com.tetrova_flowledger.data.currency.ExchangeRateRepository
import com.tetrova_flowledger.data.local.dao.ExpenseDao
import com.tetrova_flowledger.data.local.dao.GoalDao
import com.tetrova_flowledger.data.local.dao.IncomeDao
import com.tetrova_flowledger.data.preferences.UserPreferencesRepository
import com.tetrova_flowledger.data.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val authSessionManager: AuthSessionManager,
    private val engine: AssistantEngine,
) : ViewModel() {
    private val welcomeMessage = AssistantMessage(
        id = UUID.randomUUID().toString(),
        role = AssistantMessageRole.ASSISTANT,
        text = "I’m FlowLedger AI. Ask about income, expenses, free cash, spending trends, or your goals.",
        timestamp = System.currentTimeMillis(),
    )

    private var memory = AssistantConversationMemory()

    private val snapshotFlow = authSessionManager.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(null)
        } else {
            combine(
                incomeDao.observeAll(user.uid),
                expenseDao.observeAll(user.uid),
                goalDao.observeAllGoals(user.uid),
                userPreferencesRepository.preferredCurrency,
                exchangeRateRepository.ratesToLkr,
            ) { incomes, expenses, goals, preferredCurrency, rates ->
                AssistantFinanceSnapshot(
                    incomes = incomes,
                    expenses = expenses,
                    goals = goals,
                    preferredCurrency = preferredCurrency,
                    ratesToLkr = rates,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    private val _uiState = MutableStateFlow(
        AssistantUiState(messages = listOf(welcomeMessage)),
    )
    val uiState: StateFlow<AssistantUiState> = _uiState

    fun openAssistant() {
        _uiState.update { it.copy(isOpen = true) }
    }

    fun closeAssistant() {
        _uiState.update { it.copy(isOpen = false) }
    }

    fun updateDraft(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.draft.trim()
        if (prompt.isBlank()) return

        val userMessage = AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = AssistantMessageRole.USER,
            text = prompt,
            timestamp = System.currentTimeMillis(),
        )

        val reply = engine.reply(
            input = prompt,
            snapshot = snapshotFlow.value,
            memory = memory,
        )

        val assistantMessage = AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = AssistantMessageRole.ASSISTANT,
            text = reply.message,
            timestamp = System.currentTimeMillis(),
        )
        memory = reply.memory

        _uiState.update {
            it.copy(
                draft = "",
                messages = it.messages + userMessage + assistantMessage,
            )
        }
    }
}
