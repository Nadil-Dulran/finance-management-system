package com.example.finance_management_system.tests.chatbot

import com.example.finance_management_system.assistant.AssistantConversationMemory
import com.example.finance_management_system.assistant.AssistantEngine
import com.example.finance_management_system.assistant.AssistantFinanceSnapshot
import com.example.finance_management_system.assistant.AssistantTopic
import com.example.finance_management_system.data.local.entity.GoalEntity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantGoalQueryTest {
    private val assistantEngine = AssistantEngine()

    @Test
    fun whatIsMyGoal_returnsGoalDetailsFromSnapshot() {
        val snapshot = AssistantFinanceSnapshot(
            incomes = emptyList(),
            expenses = emptyList(),
            goals = listOf(
                goalEntity(
                    id = "goal-1",
                    title = "Laptop",
                    targetAmountLkr = 100_000.0,
                    currentSavedLkr = 20_000.0,
                    monthlyContributionLkr = 10_000.0,
                    deadlineAt = monthsFromNow(8),
                ),
            ),
            preferredCurrency = "LKR",
            ratesToLkr = mapOf("LKR" to 1.0),
        )

        val reply = assistantEngine.reply(
            input = "what is my goal?",
            snapshot = snapshot,
            memory = AssistantConversationMemory(),
        )

        assertTrue(reply.message.contains("Goal: Laptop."))
        assertTrue(reply.message.contains("Saved: LKR 20,000.00."))
        assertTrue(reply.message.contains("Left: LKR 80,000.00."))
        assertTrue(reply.message.contains("Deadline: 8 months remaining."))
        assertEquals(AssistantTopic.GOAL, reply.memory.currentTopic)
        assertEquals("goal-1", reply.memory.selectedGoalId)
    }
}

private fun goalEntity(
    id: String,
    title: String,
    targetAmountLkr: Double,
    currentSavedLkr: Double,
    monthlyContributionLkr: Double,
    deadlineAt: Long,
): GoalEntity {
    return GoalEntity(
        id = id,
        userId = "user-1",
        title = title,
        targetAmountLkr = targetAmountLkr,
        currentSavedLkr = currentSavedLkr,
        monthlyContributionLkr = monthlyContributionLkr,
        contributionDayOfMonth = 1,
        contributionSource = "Salary",
        allowEmergencyUse = true,
        emergencyUsedLkr = 0.0,
        lastContributionAt = 0L,
        deadlineAt = deadlineAt,
        createdAt = 0L,
    )
}

private fun monthsFromNow(months: Int): Long {
    return Calendar.getInstance().apply {
        add(Calendar.MONTH, months)
    }.timeInMillis
}
