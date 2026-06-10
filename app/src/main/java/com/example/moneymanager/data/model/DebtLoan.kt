package com.example.moneymanager.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

data class DebtLoan(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val personName: String = "",
    val amount: Double = 0.0,
    var paidAmount: Double = 0.0,
    val type: String = "debt", // "debt" or "loan"
    val description: String = "",
    val dueDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    
    @get:PropertyName("isResolved") // Getter for Firestore
    @set:PropertyName("isResolved") // Setter for Firestore
    var isResolved: Boolean = false
) {
    @get:Exclude // Exclude from direct Firestore mapping if you only want to use the primary 'isResolved' field
    val remainingAmount: Double
        get() = (amount - paidAmount).coerceAtLeast(0.0)
}
