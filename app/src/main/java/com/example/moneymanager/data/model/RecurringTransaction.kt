package com.example.moneymanager.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class RecurringTransaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "expense", // "income" or "expense"
    val category: String = "",
    val description: String = "",
    val frequency: String = "monthly", // "daily", "weekly", "monthly", "yearly"
    val startDate: Timestamp = Timestamp.now(),
    val nextOccurrence: Timestamp = Timestamp.now(),
    val isActive: Boolean = true
)
