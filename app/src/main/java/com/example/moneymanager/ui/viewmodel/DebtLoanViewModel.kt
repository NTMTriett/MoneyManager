package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.DebtLoan
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.data.repository.DebtLoanRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DebtLoanViewModel @Inject constructor(
    private val repository: DebtLoanRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _debtLoansState = MutableStateFlow<List<DebtLoan>>(emptyList())
    val debtLoansState: StateFlow<List<DebtLoan>> = _debtLoansState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadDebtLoans()
    }

    private fun loadDebtLoans() {
        viewModelScope.launch {
            repository.getDebtLoans().collect { list ->
                _debtLoansState.value = list
            }
        }
    }

    fun addDebtLoan(debtLoan: DebtLoan) {
        viewModelScope.launch {
            repository.addDebtLoan(debtLoan).onSuccess {
                val calendar = Calendar.getInstance()
                val transaction = Transaction(
                    amount = debtLoan.amount,
                    type = if (debtLoan.type == "debt") "income" else "expense",
                    category = "Debt/Loan",
                    description = "${if (debtLoan.type == "debt") "Borrowed from" else "Lent to"} ${debtLoan.personName}",
                    date = Timestamp.now(),
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                transactionRepository.addTransaction(transaction).onSuccess {
                    _uiEvent.emit("Thêm giao dịch thành công")
                }.onFailure {
                    _uiEvent.emit("Lỗi cập nhật số dư: ${it.message}")
                }
            }.onFailure {
                Log.e("DebtLoanVM", "Add failed", it)
                _uiEvent.emit("Lỗi: ${it.message}")
            }
        }
    }

    fun deleteDebtLoan(id: String) {
        viewModelScope.launch {
            repository.deleteDebtLoan(id).onSuccess {
                _uiEvent.emit("Đã xóa bản ghi")
            }.onFailure {
                _uiEvent.emit("Lỗi khi xóa: ${it.message}")
            }
        }
    }

    fun resolveDebtLoan(debtLoan: DebtLoan) {
        if (debtLoan.id.isBlank()) return
        
        viewModelScope.launch {
            repository.resolveDebtLoan(debtLoan.id).onSuccess {
                val calendar = Calendar.getInstance()
                val transaction = Transaction(
                    amount = debtLoan.amount,
                    // Nếu trước đó mượn (income) -> Giờ trả (expense)
                    // Nếu trước đó cho vay (expense) -> Giờ thu hồi (income)
                    type = if (debtLoan.type == "debt") "expense" else "income",
                    category = "Debt/Loan",
                    description = "Resolved: ${debtLoan.personName}",
                    date = Timestamp.now(),
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                transactionRepository.addTransaction(transaction).onSuccess {
                    _uiEvent.emit("Đã hoàn tất và cập nhật số dư")
                }.onFailure {
                    _uiEvent.emit("Ghi chú: Đã hoàn thành nhưng lỗi cập nhật số dư")
                }
            }.onFailure {
                _uiEvent.emit("Lỗi khi thực hiện: ${it.message}")
            }
        }
    }
}
