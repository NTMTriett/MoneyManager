package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.SavingsGoal
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.data.repository.SavingsGoalRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    private val repository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _goalsState = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val goalsState: StateFlow<List<SavingsGoal>> = _goalsState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            repository.getSavingsGoals().collect { goals ->
                _goalsState.value = goals
            }
        }
    }

    fun addGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.addSavingsGoal(goal).onSuccess {
                _uiEvent.emit("Đã tạo mục tiêu tiết kiệm")
            }.onFailure {
                _uiEvent.emit("Lỗi: ${it.message}")
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goalId).onSuccess {
                _uiEvent.emit("Đã xóa mục tiêu")
            }
        }
    }

    fun addFunds(goalId: String, amount: Double, goalName: String) {
        viewModelScope.launch {
            repository.addFundsToGoal(goalId, amount).onSuccess {
                val calendar = Calendar.getInstance()
                val transaction = Transaction(
                    amount = amount,
                    type = "expense",
                    category = "Savings",
                    description = "Deposit to goal: $goalName",
                    date = Timestamp.now(),
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                transactionRepository.addTransaction(transaction).onSuccess {
                    _uiEvent.emit("Thêm giao dịch thành công")
                }
            }.onFailure {
                _uiEvent.emit("Lỗi: ${it.message}")
            }
        }
    }
}
