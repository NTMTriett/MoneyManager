package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getSavingsGoals(): Flow<List<SavingsGoal>>
    suspend fun addSavingsGoal(goal: SavingsGoal): Result<Unit>
    suspend fun updateSavingsGoal(goal: SavingsGoal): Result<Unit>
    suspend fun deleteSavingsGoal(goalId: String): Result<Unit>
    suspend fun addFundsToGoal(goalId: String, amount: Double): Result<Unit>
}
