package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun addRecurringTransaction(transaction: RecurringTransaction): Result<Unit>
    suspend fun updateRecurringTransaction(transaction: RecurringTransaction): Result<Unit>
    suspend fun deleteRecurringTransaction(id: String): Result<Unit>
}
