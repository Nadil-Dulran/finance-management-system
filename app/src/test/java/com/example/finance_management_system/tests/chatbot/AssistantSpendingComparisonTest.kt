package com.example.finance_management_system.tests.chatbot

import com.example.finance_management_system.assistant.AssistantConversationMemory
import com.example.finance_management_system.assistant.AssistantEngine
import com.example.finance_management_system.assistant.AssistantFinanceSnapshot
import com.example.finance_management_system.assistant.AssistantTopic
import com.example.finance_management_system.assistant.ComparisonMetric
import com.example.finance_management_system.assistant.ComparisonPeriod
import com.example.finance_management_system.data.local.entity.ExpenseEntity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantSpendingComparisonTest {
    private val assistantEngine = AssistantEngine()

    @Test
    fun compareSpendingWithLastMonth_returnsMonthlyComparisonDetails() {
        val snapshot = AssistantFinanceSnapshot(
            incomes = emptyList(),
            expenses = listOf(
                expenseEntity(
                    id = "expense-current",
                    category = "Groceries",
                    amountLkr = 12_000.0,
                    spentAt = currentMonthTimestamp(dayOfMonth = 10),
                ),
                expenseEntity(
                    id = "expense-previous",
                    category = "Groceries",
                    amountLkr = 8_000.0,
                    spentAt = previousMonthTimestamp(dayOfMonth = 10),
                ),
            ),
            goals = emptyList(),
            preferredCurrency = "LKR",
            ratesToLkr = mapOf("LKR" to 1.0),
        )

        val reply = assistantEngine.reply(
            input = "compare spending with last month",
            snapshot = snapshot,
            memory = AssistantConversationMemory(),
        )

        assertTrue(reply.message.contains("This month spending is LKR 12,000.00."))
        assertTrue(reply.message.contains("Compared with last month, it is up."))
        assertTrue(reply.message.contains("+LKR 4,000.00 and +50.0% vs Last month."))
        assertEquals(AssistantTopic.COMPARISON, reply.memory.currentTopic)
        assertEquals(ComparisonMetric.EXPENSES, reply.memory.comparisonMetric)
        assertEquals(ComparisonPeriod.MONTH, reply.memory.comparisonPeriod)
    }
}

private fun expenseEntity(
    id: String,
    category: String,
    amountLkr: Double,
    spentAt: Long,
): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        userId = "user-1",
        category = category,
        spendingType = "Discretionary",
        recurrenceType = "None",
        isRecurringTemplate = false,
        originalCurrency = "LKR",
        originalAmount = amountLkr,
        amountLkr = amountLkr,
        paymentMethod = "Card",
        accountName = "Main",
        note = "",
        spentAt = spentAt,
        createdAt = spentAt,
    )
}

private fun currentMonthTimestamp(dayOfMonth: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun previousMonthTimestamp(dayOfMonth: Int): Long {
    return Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
