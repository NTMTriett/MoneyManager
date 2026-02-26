package com.example.moneymanager.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.moneymanager.data.model.Transaction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private val transactionsCollection = firestore.collection("transactions")

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val transactions = snapshot.toObjects(Transaction::class.java)
                        .sortedByDescending { it.date.toDate() }
                    trySend(transactions)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.toObjects(Transaction::class.java)
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getTransactionsByMonth(month: Int, year: Int): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.toObjects(Transaction::class.java)
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun getTransactionsByTypeAndMonth(type: String, yearMonth: YearMonth): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val month = yearMonth.monthValue
        val year = yearMonth.year

        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null ) {
                    val transactions = snapshot.toObjects(Transaction::class.java)
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val transactions = snapshot.toObjects(Transaction::class.java)
                        .sortedByDescending { it.date.toDate() }
                        .take(limit)
                    trySend(transactions)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val newTransaction = transaction.copy(userId = userId)
            val documentRef = transactionsCollection.document()
            val transactionWithId = newTransaction.copy(id = documentRef.id)
            documentRef.set(transactionWithId).await()
            Result.success(transactionWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            if (transaction.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Transaction ID cannot be empty"))
            }
            
            val updatedTransaction = transaction.copy(userId = userId)
            transactionsCollection.document(transaction.id).set(updatedTransaction).await()
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            transactionsCollection.document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactionById(transactionId: String): Result<Transaction> {
        return try {
            val document = transactionsCollection.document(transactionId).get().await()
            if (document.exists()) {
                val transaction = document.toObject(Transaction::class.java)
                if (transaction != null) {
                    Result.success(transaction)
                } else {
                    Result.failure(Exception("Failed to convert document to Transaction"))
                }
            } else {
                Result.failure(Exception("Transaction not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}