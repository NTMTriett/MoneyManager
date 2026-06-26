package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.DebtLoan
import kotlinx.coroutines.flow.Flow

interface DebtLoanRepository {
    fun getDebtLoans(): Flow<List<DebtLoan>>
    suspend fun addDebtLoan(debtLoan: DebtLoan): Result<Unit>
    suspend fun updateDebtLoan(debtLoan: DebtLoan): Result<Unit>
    suspend fun deleteDebtLoan(id: String): Result<Unit>
}
