package com.example.finance_management_system.repository.local

import com.example.finance_management_system.data.currency.CurrencyConverter
import com.example.finance_management_system.data.currency.ExchangeRateRepository
import com.example.finance_management_system.data.local.dao.GoalDao
import com.example.finance_management_system.data.local.entity.GoalEntity
import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import com.example.finance_management_system.data.session.AuthSessionManager
import com.example.finance_management_system.data.sync.SyncEntityType
import com.example.finance_management_system.data.sync.SyncQueueManager
import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.repository.GoalRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LocalGoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val authSessionManager: AuthSessionManager,
    private val syncQueueManager: SyncQueueManager,
) : GoalRepository {
    override fun observePrimaryGoal(): Flow<GoalOverview?> {
        return authSessionManager.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                combine(
                    goalDao.observePrimaryGoal(user.uid),
                    userPreferencesRepository.preferredCurrency,
                    exchangeRateRepository.ratesToLkr,
                ) { goal, preferredCurrency, rates ->
                    goal?.toOverview(preferredCurrency, rates)
                }
            }
        }
    }

    override fun observeGoals(): Flow<List<GoalOverview>> {
        return authSessionManager.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                combine(
                    goalDao.observeAllGoals(user.uid),
                    userPreferencesRepository.preferredCurrency,
                    exchangeRateRepository.ratesToLkr,
                ) { goals, preferredCurrency, rates ->
                    goals.map { it.toOverview(preferredCurrency, rates) }
                }
            }
        }
    }

    override suspend fun addGoal(
        title: String,
        targetAmount: Double,
        currentSaved: Double,
        monthsToDeadline: Int,
        monthlyContribution: Double,
        contributionDayOfMonth: Int,
        allowEmergencyUse: Boolean,
    ): Result<Unit> = runCatching {
        val userId = authSessionManager.currentUserValue?.uid ?: error("No signed-in user.")
        val goal = GoalEntity(
            id = "goal_${UUID.randomUUID()}",
            userId = userId,
            title = title.trim(),
            targetAmountLkr = targetAmount,
            currentSavedLkr = currentSaved,
            monthlyContributionLkr = monthlyContribution.coerceAtLeast(0.0),
            contributionDayOfMonth = contributionDayOfMonth.coerceIn(1, 28),
            contributionSource = "Salary",
            allowEmergencyUse = allowEmergencyUse,
            emergencyUsedLkr = 0.0,
            lastContributionAt = 0L,
            deadlineAt = monthsFromNow(monthsToDeadline),
            createdAt = System.currentTimeMillis(),
        )
        goalDao.upsert(goal)
        syncQueueManager.enqueueUpsert(userId, SyncEntityType.GOAL, goal.id)
    }

    override suspend fun updateGoal(
        goalId: String,
        title: String,
        targetAmount: Double,
        currentSaved: Double,
        monthsToDeadline: Int,
        monthlyContribution: Double,
        contributionDayOfMonth: Int,
        allowEmergencyUse: Boolean,
    ): Result<Unit> = runCatching {
        val userId = authSessionManager.currentUserValue?.uid ?: error("No signed-in user.")
        val existing = goalDao.getGoalById(goalId, userId) ?: error("Goal not found.")
        val updated = existing.copy(
            title = title.trim(),
            targetAmountLkr = targetAmount,
            currentSavedLkr = currentSaved,
            monthlyContributionLkr = monthlyContribution.coerceAtLeast(0.0),
            contributionDayOfMonth = contributionDayOfMonth.coerceIn(1, 28),
            allowEmergencyUse = allowEmergencyUse,
            deadlineAt = monthsFromNow(monthsToDeadline),
        )
        goalDao.upsert(updated)
        syncQueueManager.enqueueUpsert(userId, SyncEntityType.GOAL, updated.id)
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> = runCatching {
        val userId = authSessionManager.currentUserValue?.uid ?: error("No signed-in user.")
        val goal = goalDao.getGoalById(goalId, userId) ?: error("Goal not found.")
        goalDao.delete(goal)
        syncQueueManager.enqueueDelete(userId, SyncEntityType.GOAL, goalId)
    }

    override suspend fun applyEmergencyWithdrawal(goalId: String, amount: Double): Result<Unit> = runCatching {
        val userId = authSessionManager.currentUserValue?.uid ?: error("No signed-in user.")
        val goal = goalDao.getGoalById(goalId, userId) ?: error("Goal not found.")
        require(goal.allowEmergencyUse) { "Emergency use is disabled for this goal." }
        require(amount > 0.0) { "Enter a valid withdrawal amount." }

        val updated = goal.copy(
            currentSavedLkr = max(goal.currentSavedLkr - amount, 0.0),
            emergencyUsedLkr = goal.emergencyUsedLkr + amount,
        )
        goalDao.upsert(updated)
        syncQueueManager.enqueueUpsert(userId, SyncEntityType.GOAL, updated.id)
    }

    override suspend fun refreshGoalContributionsIfNeeded() {
        val now = Calendar.getInstance()
        val userId = authSessionManager.currentUserValue?.uid ?: return
        val goals = goalDao.getAllGoals(userId)
        goals.forEach { goal ->
            if (goal.monthlyContributionLkr <= 0.0) return@forEach
            val lastContributionMonth = goal.lastContributionAt.takeIf { it > 0L }?.let {
                Calendar.getInstance().apply { timeInMillis = it }
            }

            val alreadyContributedThisMonth = lastContributionMonth?.let {
                it.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        it.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            } ?: false

            if (!alreadyContributedThisMonth && now.get(Calendar.DAY_OF_MONTH) >= goal.contributionDayOfMonth) {
                val updated = goal.copy(
                    currentSavedLkr = goal.currentSavedLkr + goal.monthlyContributionLkr,
                    lastContributionAt = now.timeInMillis,
                )
                goalDao.upsert(updated)
                syncQueueManager.enqueueUpsert(userId, SyncEntityType.GOAL, updated.id)
            }
        }
    }

    override suspend fun seedDemoGoalIfNeeded() {
        // Goals should start empty so users can define their own saving plans.
    }

    suspend fun cleanupLegacyDemoGoals() {
        val userId = authSessionManager.currentUserValue?.uid ?: return
        val legacyGoals = goalDao.getAllGoals(userId).filter { goal ->
            val title = goal.title.lowercase()
            title.contains("mac") || title.contains("m4")
        }
        legacyGoals.forEach { goal ->
            goalDao.delete(goal)
            runCatching { syncQueueManager.enqueueDelete(userId, SyncEntityType.GOAL, goal.id) }
        }
    }

    private fun GoalEntity.toOverview(
        preferredCurrency: String,
        rates: Map<String, Double>,
    ): GoalOverview {
        val remainingLkr = max(targetAmountLkr - currentSavedLkr, 0.0)
        val monthsRemaining = monthsUntil(deadlineAt).coerceAtLeast(1)
        val progress = if (targetAmountLkr <= 0.0) 0f else (currentSavedLkr / targetAmountLkr).toFloat().coerceIn(0f, 1f)

        return GoalOverview(
            id = id,
            title = title,
            targetAmountLabel = formatDisplayCurrency(targetAmountLkr, preferredCurrency, rates),
            currentSavedLabel = formatDisplayCurrency(currentSavedLkr, preferredCurrency, rates),
            remainingAmountLabel = formatDisplayCurrency(remainingLkr, preferredCurrency, rates),
            deadlineLabel = "$monthsRemaining months remaining",
            monthlyNeedLabel = "Need about ${formatDisplayCurrency(remainingLkr / monthsRemaining, preferredCurrency, rates)} per month",
            monthlyContributionLabel = formatDisplayCurrency(monthlyContributionLkr, preferredCurrency, rates),
            contributionScheduleLabel = "Auto-save on day $contributionDayOfMonth from $contributionSource",
            emergencyUseLabel = if (allowEmergencyUse) "Emergency use is allowed" else "Emergency use is locked",
            emergencyUsedLabel = if (emergencyUsedLkr > 0.0) {
                "Emergency use so far: ${formatDisplayCurrency(emergencyUsedLkr, preferredCurrency, rates)}"
            } else {
                "No emergency withdrawals yet"
            },
            targetAmountLkr = targetAmountLkr,
            currentSavedLkr = currentSavedLkr,
            monthlyContributionLkr = monthlyContributionLkr,
            contributionDayOfMonth = contributionDayOfMonth,
            monthsRemaining = monthsRemaining,
            allowEmergencyUse = allowEmergencyUse,
            progress = progress,
            isCompleted = remainingLkr <= 0.0,
        )
    }

    private fun monthsFromNow(months: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.MONTH, months.coerceAtLeast(1))
        }.timeInMillis
    }

    private fun monthsUntil(deadlineAt: Long): Int {
        val now = Calendar.getInstance()
        val deadline = Calendar.getInstance().apply { timeInMillis = deadlineAt }
        val yearDiff = deadline.get(Calendar.YEAR) - now.get(Calendar.YEAR)
        val monthDiff = deadline.get(Calendar.MONTH) - now.get(Calendar.MONTH)
        return yearDiff * 12 + monthDiff
    }

    private fun formatDisplayCurrency(
        amountLkr: Double,
        preferredCurrency: String,
        rates: Map<String, Double>,
    ): String {
        val currencyCode = preferredCurrency.uppercase()
        if (currencyCode == "LKR") {
            return CurrencyConverter.format(amountLkr, currencyCode)
        }

        val rateToLkr = rates[currencyCode] ?: return CurrencyConverter.format(amountLkr, "LKR")
        val converted = CurrencyConverter.fromLkr(amountLkr, rateToLkr)
        return CurrencyConverter.format(converted, currencyCode)
    }
}
