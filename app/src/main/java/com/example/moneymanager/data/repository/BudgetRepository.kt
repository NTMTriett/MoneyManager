package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date
import kotlinx.coroutines.flow.first

/**
 * Interface for managing budget data.
 */
interface BudgetRepository {

    /**
     * Retrieves a flow of budgets for a given date.
     */
    fun getBudgets(date: Date): Flow<List<Budget>>

    /**
     * Saves a new budget.
     */
    suspend fun saveBudget(budget: Budget): Result<Unit>

    /**
     * Updates an existing budget.
     */
    suspend fun updateBudget(budget: Budget): Result<Unit>

    /**
     * Deletes a budget by its ID.
     */
    suspend fun deleteBudget(budgetId: String): Result<Unit>

    /**
     * Returns the list of currently active budgets (today falls within startDate..endDate).
     * Used to provide budget context to AI for smarter categorization.
     */
    suspend fun getActiveBudgets(): List<Budget> = getBudgets(Date()).first()
}
