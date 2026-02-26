package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByType(type: String): Flow<List<Transaction>>
    fun getTransactionsByMonth(month: Int, year: Int): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    fun getTransactionsByTypeAndMonth(type: String, yearMonth: YearMonth): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction): Result<Transaction>
    suspend fun updateTransaction(transaction: Transaction): Result<Transaction>
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    suspend fun getTransactionById(transactionId: String): Result<Transaction>

}