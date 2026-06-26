package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.RecurringTransaction
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.data.repository.RecurringTransactionRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class RecurringTransactionViewModel @Inject constructor(
    private val repository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _recurringState = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val recurringState: StateFlow<List<RecurringTransaction>> = _recurringState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadRecurring()
    }

    private fun loadRecurring() {
        viewModelScope.launch {
            repository.getRecurringTransactions().collect { list ->
                _recurringState.value = list
            }
        }
    }

    fun addRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch {
            repository.addRecurringTransaction(recurring).onSuccess {
                val calendar = Calendar.getInstance()
                val transaction = Transaction(
                    amount = recurring.amount,
                    type = recurring.type,
                    category = recurring.category,
                    description = "[Recurring] ${recurring.description}",
                    date = Timestamp.now(),
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                transactionRepository.addTransaction(transaction).onSuccess {
                    _uiEvent.emit("Thêm giao dịch thành công")
                }.onFailure {
                    _uiEvent.emit("Lỗi khi tạo giao dịch đầu tiên: ${it.message}")
                }
            }.onFailure {
                _uiEvent.emit("Lỗi khi thêm: ${it.message}")
            }
        }
    }

    fun updateRecurring(transaction: RecurringTransaction) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(transaction).onSuccess {
                _uiEvent.emit("Cập nhật thành công")
            }
        }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id).onSuccess {
                _uiEvent.emit("Đã xóa")
            }
        }
    }
}
