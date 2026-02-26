package com.example.moneymanager.data.model

import com.google.firebase.firestore.DocumentId

    data class Category(
        @DocumentId
        val id: String = "",
        val userId: String = "",
        val name: String = "",
        val type: String = "", // "income" or "expense"
        val parentId: String? = null // null = top-level category
    )

