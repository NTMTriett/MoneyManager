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
                    _uiEvent.emit("Record added and balance updated")
                }.onFailure {
                    _uiEvent.emit("Record added but failed to update balance")
                }
            }.onFailure {
                Log.e("DebtLoanVM", "Add failed", it)
                _uiEvent.emit("Error: ${it.message}")
            }
        }
    }

    fun updateDebtLoan(debtLoan: DebtLoan) {
        viewModelScope.launch {
            repository.updateDebtLoan(debtLoan).onSuccess {
                _uiEvent.emit("Record updated successfully")
            }.onFailure {
                _uiEvent.emit("Update failed: ${it.message}")
            }
        }
    }

    fun payDebtLoan(debtLoan: DebtLoan, paymentAmount: Double) {
        viewModelScope.launch { // Ensure the whole function runs in a coroutine
            if (debtLoan.id.isBlank() || paymentAmount <= 0) {
                _uiEvent.emit("Invalid payment amount or record ID.")
                return@launch
            }
            
            val newPaidAmount = (debtLoan.paidAmount + paymentAmount).coerceAtMost(debtLoan.amount)
            val isNowResolved = newPaidAmount >= debtLoan.amount
            
            val updatedItem = debtLoan.copy(
                paidAmount = newPaidAmount,
                isResolved = isNowResolved
            )
            
            repository.updateDebtLoan(updatedItem).onSuccess {
                val calendar = Calendar.getInstance()
                val transaction = Transaction(
                    amount = paymentAmount,
                    type = if (debtLoan.type == "debt") "expense" else "income", // If you borrowed (debt) -> paying is expense. If you lent (loan) -> receiving is income.
                    category = "Debt/Loan",
                    description = "Payment from/to ${debtLoan.personName} for ${debtLoan.description}",
                    date = Timestamp.now(),
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                // No .await() on Result
                transactionRepository.addTransaction(transaction)
                    .onSuccess { _uiEvent.emit(if (isNowResolved) "Record fully settled!" else "Payment recorded") }
                    .onFailure { _uiEvent.emit("Payment recorded but failed to update balance: ${it.message}") }
            }.onFailure {
                _uiEvent.emit("Payment failed: ${it.message}")
            }
        }
    }

    fun deleteDebtLoan(id: String) {
        viewModelScope.launch {
            repository.deleteDebtLoan(id).onSuccess {
                _uiEvent.emit("Record deleted")
            }.onFailure {
                _uiEvent.emit("Delete failed: ${it.message}")
            }
        }
    }

    fun resolveDebtLoan(debtLoan: DebtLoan) {
        viewModelScope.launch { // Ensure the whole function runs in a coroutine
            if (debtLoan.id.isBlank()) {
                _uiEvent.emit("Invalid record ID.")
                return@launch
            }
            // Call payDebtLoan with the remaining amount to fully settle
            val amountToSettle = debtLoan.remainingAmount
            if (amountToSettle > 0) {
                payDebtLoan(debtLoan, amountToSettle)
            } else if (!debtLoan.isResolved) {
                // If already paid in full but not marked resolved (edge case)
                repository.updateDebtLoan(debtLoan.copy(isResolved = true)).onSuccess {
                    _uiEvent.emit("Record marked as resolved")
                }.onFailure {
                    _uiEvent.emit("Failed to mark as resolved: ${it.message}")
                }
            }
        }
    }
}
