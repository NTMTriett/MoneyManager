package com.example.moneymanager.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SavingsGoal(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    val progress: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat() else 0f
    
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}
