package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.DebtLoan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDebtLoanRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DebtLoanRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    private val debtLoansCollection = firestore.collection("debt_loans")

    override fun getDebtLoans(): Flow<List<DebtLoan>> {
        return debtLoansCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(DebtLoan::class.java) }
    }

    override suspend fun addDebtLoan(debtLoan: DebtLoan): Result<Unit> = try {
        val newDebtLoan = debtLoan.copy(userId = userId)
        val docRef = debtLoansCollection.document()
        // Ensure paidAmount is initialized to 0.0 for new debts/loans
        docRef.set(newDebtLoan.copy(id = docRef.id, paidAmount = 0.0, isResolved = false)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateDebtLoan(debtLoan: DebtLoan): Result<Unit> = try {
        debtLoansCollection.document(debtLoan.id).set(debtLoan).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteDebtLoan(id: String): Result<Unit> = try {
        debtLoansCollection.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
