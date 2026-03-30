package com.example.moneymanager.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class DebtLoan(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val personName: String = "",
    val amount: Double = 0.0,
    val type: String = "debt", // "debt" (mình nợ người ta) or "loan" (người ta nợ mình)
    val description: String = "",
    val dueDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val isResolved: Boolean = false
)
