package com.example.finance_management_system.data.remote

import com.example.finance_management_system.data.local.dao.ExpenseDao
import com.example.finance_management_system.data.local.dao.GoalDao
import com.example.finance_management_system.data.local.dao.IncomeDao
import com.example.finance_management_system.data.local.entity.ExpenseEntity
import com.example.finance_management_system.data.local.entity.GoalEntity
import com.example.finance_management_system.data.local.entity.IncomeEntity
import com.example.finance_management_system.data.remote.model.FirestoreExpense
import com.example.finance_management_system.data.remote.model.FirestoreGoal
import com.example.finance_management_system.data.remote.model.FirestoreIncome
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSyncService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
) {
    suspend fun syncUserData(userId: String) {
        Log.d(TAG, "Starting full user sync for uid=$userId")
        ensureUserDocument(userId)
        syncIncome(userId)
        syncExpenses(userId)
        syncGoals(userId)
        Log.d(TAG, "Completed full user sync for uid=$userId")
    }

    suspend fun pushIncome(userId: String, entity: IncomeEntity) {
        ensureUserDocument(userId)
        userCollection(userId)
            .collection(COLLECTION_INCOME)
            .document(entity.id)
            .set(entity.toFirestore())
            .await()
    }

    suspend fun pushExpense(userId: String, entity: ExpenseEntity) {
        ensureUserDocument(userId)
        userCollection(userId)
            .collection(COLLECTION_EXPENSES)
            .document(entity.id)
            .set(entity.toFirestore())
            .await()
    }

    suspend fun pushGoal(userId: String, goal: GoalEntity) {
        ensureUserDocument(userId)
        userCollection(userId)
            .collection(COLLECTION_GOALS)
            .document(goal.id)
            .set(goal.toFirestore())
            .await()
    }

    suspend fun deleteIncome(userId: String, incomeId: String) {
        userCollection(userId)
            .collection(COLLECTION_INCOME)
            .document(incomeId)
            .delete()
            .await()
    }

    suspend fun deleteExpense(userId: String, expenseId: String) {
        userCollection(userId)
            .collection(COLLECTION_EXPENSES)
            .document(expenseId)
            .delete()
            .await()
    }

    suspend fun deleteGoal(userId: String, goalId: String) {
        userCollection(userId)
            .collection(COLLECTION_GOALS)
            .document(goalId)
            .delete()
            .await()
    }

    suspend fun deleteUserData(userId: String) {
        val userDoc = userCollection(userId)
        userDoc.collection(COLLECTION_INCOME).get().await().documents.forEach { it.reference.delete().await() }
        userDoc.collection(COLLECTION_EXPENSES).get().await().documents.forEach { it.reference.delete().await() }
        userDoc.collection(COLLECTION_GOALS).get().await().documents.forEach { it.reference.delete().await() }
        userDoc.delete().await()
    }

    private suspend fun ensureUserDocument(userId: String) {
        val user = firebaseAuth.currentUser
        val userData = mapOf(
            "email" to (user?.email ?: ""),
            "createdAt" to FieldValue.serverTimestamp(),
        )

        userCollection(userId)
            .set(userData, SetOptions.merge())
            .await()
    }

    private suspend fun syncIncome(userId: String) {
        val collection = userCollection(userId).collection(COLLECTION_INCOME)
        val remote = collection.get().await().documents.mapNotNull { doc ->
            doc.toObject(FirestoreIncome::class.java)?.copy(id = doc.id)
        }

        Log.d(TAG, "syncIncome: remoteCount=${remote.size} for uid=$userId")

        // Upsert remote to local
        if (remote.isNotEmpty()) {
            incomeDao.upsertAll(remote.map { it.toEntity(userId) })
        }

        // Upload any local entries missing remotely
        val remoteIds = remote.map { it.id }.toSet()
        incomeDao.getAll(userId).filter { it.id !in remoteIds }.forEach { entity ->
            Log.d(TAG, "syncIncome: uploading missing local income id=${entity.id}")
            collection.document(entity.id).set(entity.toFirestore()).await()
        }
    }

    private suspend fun syncExpenses(userId: String) {
        val collection = userCollection(userId).collection(COLLECTION_EXPENSES)
        val remote = collection.get().await().documents.mapNotNull { doc ->
            doc.toObject(FirestoreExpense::class.java)?.copy(id = doc.id)
        }

        Log.d(TAG, "syncExpenses: remoteCount=${remote.size} for uid=$userId")

        if (remote.isNotEmpty()) {
            expenseDao.upsertAll(remote.map { it.toEntity(userId) })
        }

        val remoteIds = remote.map { it.id }.toSet()
        expenseDao.getAll(userId).filter { it.id !in remoteIds }.forEach { entity ->
            Log.d(TAG, "syncExpenses: uploading missing local expense id=${entity.id}")
            collection.document(entity.id).set(entity.toFirestore()).await()
        }
    }

    private suspend fun syncGoals(userId: String) {
        val collection = userCollection(userId).collection(COLLECTION_GOALS)
        val remote = collection.get().await().documents.mapNotNull { doc ->
            doc.toObject(FirestoreGoal::class.java)?.copy(id = doc.id)
        }

        Log.d(TAG, "syncGoals: remoteCount=${remote.size} for uid=$userId")

        if (remote.isNotEmpty()) {
            goalDao.upsertAll(remote.map { it.toEntity(userId) })
        }

        val remoteIds = remote.map { it.id }.toSet()
        goalDao.getAllGoals(userId).filter { it.id !in remoteIds }.forEach { goal ->
            Log.d(TAG, "syncGoals: uploading missing local goal id=${goal.id}")
            collection.document(goal.id).set(goal.toFirestore()).await()
        }
    }

    private fun userCollection(userId: String) = firestore.collection(COLLECTION_USERS).document(userId)

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_INCOME = "income_entries"
        const val COLLECTION_EXPENSES = "expense_entries"
        const val COLLECTION_GOALS = "goals"
        const val TAG = "FirestoreSyncService"
    }
}

private fun IncomeEntity.toFirestore() = FirestoreIncome(
    id = id,
    sourceType = sourceType,
    amountOriginal = amountOriginal,
    currency = currency,
    exchangeRateToLkr = exchangeRateToLkr,
    amountLkr = amountLkr,
    note = note,
    receivedAt = receivedAt,
    createdAt = createdAt,
)

private fun FirestoreIncome.toEntity(userId: String) = IncomeEntity(
    id = id,
    userId = userId,
    sourceType = sourceType,
    amountOriginal = amountOriginal,
    currency = currency,
    exchangeRateToLkr = exchangeRateToLkr,
    amountLkr = amountLkr,
    note = note,
    receivedAt = receivedAt,
    createdAt = createdAt,
)

private fun ExpenseEntity.toFirestore() = FirestoreExpense(
    id = id,
    category = category,
    spendingType = spendingType,
    recurrenceType = recurrenceType,
    recurrenceGroupId = recurrenceGroupId,
    isRecurringTemplate = isRecurringTemplate,
    originalCurrency = originalCurrency,
    originalAmount = originalAmount,
    amountLkr = amountLkr,
    paymentMethod = paymentMethod,
    accountName = accountName,
    note = note,
    spentAt = spentAt,
    createdAt = createdAt,
)

private fun FirestoreExpense.toEntity(userId: String) = ExpenseEntity(
    id = id,
    userId = userId,
    category = category,
    spendingType = spendingType,
    recurrenceType = recurrenceType,
    recurrenceGroupId = recurrenceGroupId,
    isRecurringTemplate = isRecurringTemplate,
    originalCurrency = originalCurrency,
    originalAmount = originalAmount,
    amountLkr = amountLkr,
    paymentMethod = paymentMethod,
    accountName = accountName,
    note = note,
    spentAt = spentAt,
    createdAt = createdAt,
)

private fun GoalEntity.toFirestore() = FirestoreGoal(
    id = id,
    title = title,
    targetAmountLkr = targetAmountLkr,
    currentSavedLkr = currentSavedLkr,
    monthlyContributionLkr = monthlyContributionLkr,
    contributionDayOfMonth = contributionDayOfMonth,
    contributionSource = contributionSource,
    allowEmergencyUse = allowEmergencyUse,
    emergencyUsedLkr = emergencyUsedLkr,
    lastContributionAt = lastContributionAt,
    deadlineAt = deadlineAt,
    createdAt = createdAt,
)

private fun FirestoreGoal.toEntity(userId: String) = GoalEntity(
    id = id,
    userId = userId,
    title = title,
    targetAmountLkr = targetAmountLkr,
    currentSavedLkr = currentSavedLkr,
    monthlyContributionLkr = monthlyContributionLkr,
    contributionDayOfMonth = contributionDayOfMonth,
    contributionSource = contributionSource,
    allowEmergencyUse = allowEmergencyUse,
    emergencyUsedLkr = emergencyUsedLkr,
    lastContributionAt = lastContributionAt,
    deadlineAt = deadlineAt,
    createdAt = createdAt,
)
