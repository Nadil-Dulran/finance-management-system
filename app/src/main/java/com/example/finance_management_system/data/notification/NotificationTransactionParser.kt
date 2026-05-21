package com.example.finance_management_system.data.notification

import com.example.finance_management_system.data.local.entity.DetectedTransactionEntity
import java.util.Locale
import java.util.UUID

object NotificationTransactionParser {
    private val amountRegex = Regex("""(?i)(LKR|Rs\.?|USD)\s*([0-9,]+(?:\.[0-9]{1,2})?)""")
    private val merchantRegex = Regex("""(?i)\b(?:at|to|from)\s+([A-Za-z0-9 &._-]{3,40})""")

    fun parse(
        packageName: String,
        title: String,
        text: String,
        postedAt: Long,
        amountToLkr: (Double, String) -> Double,
    ): DetectedTransactionEntity? {
        val combined = "$title $text".trim()
        val lowered = combined.lowercase(Locale.ROOT)
        if (!looksLikeFinancialNotification(packageName, lowered)) return null

        val amountMatch = amountRegex.find(combined) ?: return null
        val currency = normalizeCurrency(amountMatch.groupValues[1])
        val amountOriginal = amountMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
        val detectedType = detectType(lowered)
        val merchant = merchantRegex.find(combined)?.groupValues?.get(1)?.trim().orEmpty()
        val suggestion = suggestCategoryOrSource(detectedType, merchant, lowered)

        return DetectedTransactionEntity(
            id = "detected_${UUID.randomUUID()}",
            userId = "",
            packageName = packageName,
            title = title.ifBlank { "Detected transaction" },
            rawText = combined,
            detectedType = detectedType,
            merchant = merchant,
            suggestedCategoryOrSource = suggestion,
            currency = currency,
            amountOriginal = amountOriginal,
            amountLkr = amountToLkr(amountOriginal, currency),
            occurredAt = postedAt,
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun looksLikeFinancialNotification(packageName: String, loweredText: String): Boolean {
        return packageName.contains("bank", ignoreCase = true) ||
                packageName.contains("wallet", ignoreCase = true) ||
                packageName.contains("pay", ignoreCase = true) ||
                listOf("credited", "debited", "spent", "purchase", "transaction", "withdrawn", "deposit").any {
                    loweredText.contains(it)
                }
    }

    private fun detectType(loweredText: String): String {
        return if (listOf("credited", "deposit", "received").any { loweredText.contains(it) }) {
            "INCOME"
        } else {
            "EXPENSE"
        }
    }

    private fun suggestCategoryOrSource(
        detectedType: String,
        merchant: String,
        loweredText: String,
    ): String {
        if (detectedType == "INCOME") {
            return when {
                loweredText.contains("salary") -> "Salary"
                loweredText.contains("freelance") -> "Freelance"
                else -> "Salary"
            }
        }

        val hint = "$merchant $loweredText".lowercase(Locale.ROOT)
        return when {
            listOf("uber", "pickme", "taxi", "fuel").any { hint.contains(it) } -> "Transport"
            listOf("cafe", "coffee", "restaurant", "kfc", "mcdonald", "pizza").any { hint.contains(it) } -> "Coffee & Dining"
            listOf("keells", "cargills", "arpico", "supermarket", "grocer").any { hint.contains(it) } -> "Groceries"
            listOf("rent").any { hint.contains(it) } -> "Rent"
            else -> "Groceries"
        }
    }

    private fun normalizeCurrency(raw: String): String = when {
        raw.equals("USD", true) -> "USD"
        else -> "LKR"
    }
}
