package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.SavingsGoal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSavingsGoalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SavingsGoalRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    private val goalsCollection = firestore.collection("savings_goals")

    override fun getSavingsGoals(): Flow<List<SavingsGoal>> {
        return goalsCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(SavingsGoal::class.java) }
    }

    override suspend fun addSavingsGoal(goal: SavingsGoal): Result<Unit> = try {
        val newGoal = goal.copy(userId = userId)
        val docRef = goalsCollection.document()
        docRef.set(newGoal.copy(id = docRef.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateSavingsGoal(goal: SavingsGoal): Result<Unit> = try {
        goalsCollection.document(goal.id).set(goal).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteSavingsGoal(goalId: String): Result<Unit> = try {
        goalsCollection.document(goalId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addFundsToGoal(goalId: String, amount: Double): Result<Unit> = try {
        val docRef = goalsCollection.document(goalId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentAmount = snapshot.getDouble("currentAmount") ?: 0.0
            transaction.update(docRef, "currentAmount", currentAmount + amount)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
