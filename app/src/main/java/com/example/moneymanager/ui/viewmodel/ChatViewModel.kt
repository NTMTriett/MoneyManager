package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.ChatMessage
import com.example.moneymanager.data.repository.BudgetRepository
import com.example.moneymanager.data.repository.OllamaRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.example.moneymanager.util.PromptUtils
import com.example.moneymanager.util.toCurrencyString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val ollamaRepository: OllamaRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private var financialContextPrompt: String = ""

    init {
        // Khởi tạo ngữ cảnh lần đầu
        updateFinancialContext()
    }

    private fun updateFinancialContext(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // 1. Tăng Timeout lên 10s để đảm bảo lấy được dữ liệu từ Firebase
                val allTransactions = withTimeoutOrNull(10000) {
                    transactionRepository.getAllTransactions().first()
                } ?: emptyList()
                
                val allBudgets = withTimeoutOrNull(5000) {
                    budgetRepository.getBudgets(Date()).first()
                } ?: emptyList()

                // 2. Tính toán số liệu
                val today = LocalDate.now()
                val totalIncome = allTransactions.filter { it.type == "income" }.sumOf { it.amount }
                val totalExpense = allTransactions.filter { it.type == "expense" }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

                val expenseByCategory = allTransactions.filter { it.type == "expense" }
                    .groupBy { it.category }
                    .mapValues { it.value.sumOf { tx -> tx.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)
                    .joinToString(", ") { "${it.first}: ${it.second.toCurrencyString()}" }

                val budgetAnalysis = if (allBudgets.isNotEmpty()) {
                    allBudgets.joinToString("\n") { budget ->
                        "- ${budget.category}: ${budget.spentAmount.toCurrencyString()} / ${budget.allocatedAmount.toCurrencyString()}"
                    }
                } else "No budgets set."

                // 3. Sử dụng lại PromptUtils như yêu cầu của bạn
                financialContextPrompt = PromptUtils.getFinancialAdvisorPrompt(
                    totalIncome, totalExpense, balance, expenseByCategory, budgetAnalysis
                )

                _chatState.value = ChatUiState.Success
                
                // Nếu đây là lần đầu mở app, hiện tin nhắn chào
                if (_messages.value.isEmpty()) {
                    addMessage(ChatMessage(content = "Hello! I'm your AI Financial Advisor. Ask me anything about your spending!", isFromUser = false))
                }
                onComplete()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Firebase Data Error: ${e.message}", e)
                _chatState.value = ChatUiState.Success
                onComplete()
            }
        }
    }

    fun sendMessage(messageContent: String) {
        if (messageContent.isBlank()) return

        addMessage(ChatMessage(content = messageContent, isFromUser = true))
        val loadingMessageId = UUID.randomUUID().toString()
        addMessage(ChatMessage(id = loadingMessageId, content = "", isFromUser = false, isLoading = true))

        viewModelScope.launch {
            // Cập nhật lại ngữ cảnh một lần nữa để lấy dữ liệu mới nhất nếu người dùng vừa add transaction
            updateFinancialContext {
                viewModelScope.launch {
                    try {
                        val prompt = PromptUtils.getChatAdvisorPrompt(financialContextPrompt, messageContent)
                        val result = ollamaRepository.sendMessage(prompt, jsonMode = false)

                        removeMessage(loadingMessageId)

                        result.fold(
                            onSuccess = { addMessage(ChatMessage(content = it.trim(), isFromUser = false)) },
                            onFailure = { addMessage(ChatMessage(content = "Connection error: ${it.message}", isFromUser = false, isError = true)) }
                        )
                    } catch (e: Exception) {
                        removeMessage(loadingMessageId)
                        addMessage(ChatMessage(content = "Error: ${e.message}", isFromUser = false, isError = true))
                    }
                }
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun removeMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
    }
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object Success : ChatUiState
    data class Error(val message: String) : ChatUiState
}
