package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.Budget
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirebaseBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BudgetRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getBudgets(date: Date): Flow<List<Budget>> {
        // --- CÁCH 1: Lọc trực tiếp trên Server (Yêu cầu phải tạo Index trên Firebase Console thành công) ---
        /*
        return firestore.collection("budgets")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("endDate", date)
            .whereLessThanOrEqualTo("startDate", date)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Budget::class.java)
            }
        */

        // --- CÁCH 2: Chỉ lấy theo userId, lọc ngày tháng tại App (An toàn, không lo lỗi Index) ---
        return firestore.collection("budgets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                val allBudgets = snapshot.toObjects(Budget::class.java)
                allBudgets.filter { budget ->
                    // Lọc những ngân sách mà ngày hiện tại nằm trong khoảng thời gian hiệu lực
                    date.time >= budget.startDate.time && date.time <= budget.endDate.time
                }
            }
    }

    override suspend fun saveBudget(budget: Budget): Result<Unit> = try {
        val uId = auth.currentUser?.uid ?: ""
        firestore.collection("budgets")
            .add(budget.copy(userId = uId))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> = try {
        firestore.collection("budgets")
            .document(budget.id)
            .set(budget)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteBudget(budgetId: String): Result<Unit> = try {
        firestore.collection("budgets")
            .document(budgetId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
