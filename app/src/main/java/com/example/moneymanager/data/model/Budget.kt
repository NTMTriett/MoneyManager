package com.example.moneymanager.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a budget for a specific category over a defined period.
 */
data class Budget(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val allocatedAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val startDate: Date = Date(),
    val endDate: Date = Date(),
) {
    val progress: Float
        get() = if (allocatedAmount > 0) (spentAmount / allocatedAmount).toFloat() else 0f

    val status: BudgetStatus
        get() = when {
            progress < 0.5f -> BudgetStatus.Normal
            progress <= 0.9f -> BudgetStatus.Warning
            else -> BudgetStatus.Over
        }
}

/**
 * Represents the different states of a budget.
 */
enum class BudgetStatus {
    Normal, // < 50%
    Warning, // 50% - 90%
    Over // > 90%
}
