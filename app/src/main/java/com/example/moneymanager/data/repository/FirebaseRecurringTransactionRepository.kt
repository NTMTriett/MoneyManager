package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.RecurringTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRecurringTransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RecurringTransactionRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    private val recurringCollection = firestore.collection("recurring_transactions")

    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(RecurringTransaction::class.java) }
    }

    override suspend fun addRecurringTransaction(transaction: RecurringTransaction): Result<Unit> = try {
        val newTransaction = transaction.copy(userId = userId)
        val docRef = recurringCollection.document()
        docRef.set(newTransaction.copy(id = docRef.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateRecurringTransaction(transaction: RecurringTransaction): Result<Unit> = try {
        recurringCollection.document(transaction.id).set(transaction).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteRecurringTransaction(id: String): Result<Unit> = try {
        recurringCollection.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
