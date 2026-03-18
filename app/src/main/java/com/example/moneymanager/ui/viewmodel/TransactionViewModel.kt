package com.example.moneymanager.ui.viewmodel

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.data.repository.CategoryRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.example.moneymanager.data.repository.OllamaRepository
import com.example.moneymanager.data.model.AiTransactionData
import com.example.moneymanager.util.PromptUtils
import com.example.moneymanager.BuildConfig
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val ollamaRepository: OllamaRepository // Inject Ollama
) : ViewModel() {

    private val _transactionsState = MutableStateFlow<TransactionsState>(TransactionsState.Loading)
    val transactionsState: StateFlow<TransactionsState> = _transactionsState.asStateFlow()

    private val _currentTransaction = MutableStateFlow<Transaction?>(null)
    val currentTransaction: StateFlow<Transaction?> = _currentTransaction.asStateFlow()

    private val _quickAddState = MutableStateFlow<QuickAddState>(QuickAddState.Idle)
    val quickAddState: StateFlow<QuickAddState> = _quickAddState.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedTransactionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<String>> = _selectedTransactionIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var _cachedTransactions: List<Transaction> = emptyList()
    private val gson = Gson()
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    init {
        loadAllTransactions()
    }

    // --- AI LOGIC ---
    fun scanBill(bitmap: Bitmap) {
        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading
            val prompt = "Extract receipt data into JSON: { amount, type, category, description }. Rules: Type must be 'income' or 'expense'. Amount must be number."
            try {
                val response = generativeModel.generateContent(content { image(bitmap); text(prompt) })
                val rawText = response.text ?: ""
                val cleanJson = rawText.substringAfter("{").substringBeforeLast("}").let { "{ $it }" }
                val aiData = gson.fromJson(cleanJson, AiTransactionData::class.java)
                saveAiTransaction(aiData)
            } catch (e: Exception) {
                _quickAddState.value = QuickAddState.Error("AI Error: ${e.message}")
            }
        }
    }

    // --- AI QUICK ADD (Dùng Ollama theo ý bạn) ---
    fun processQuickAdd(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading
            try {
                // Lấy danh sách category để AI gán cho đúng
                val categories = categoryRepository.getAllCategories().first().joinToString(",") { it.name }
                val prompt = PromptUtils.getQuickAddPrompt(input, categories)

                val result = ollamaRepository.sendMessage(prompt, modelName = "qwen2.5:3b", jsonMode = true)
                
                result.fold(
                    onSuccess = { jsonResponse ->
                        val cleanJson = jsonResponse.substringAfter("{").substringBeforeLast("}").let { "{ $it }" }
                        val aiData = gson.fromJson(cleanJson, AiTransactionData::class.java)
                        saveAiTransaction(aiData)
                    },
                    onFailure = { 
                        _quickAddState.value = QuickAddState.Error("Ollama connection error")
                    }
                )
            } catch (e: Exception) {
                _quickAddState.value = QuickAddState.Error("Process error: ${e.message}")
            }
        }
    }

    private fun saveAiTransaction(aiData: AiTransactionData) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val transaction = Transaction(
                amount = aiData.amount ?: 0.0,
                type = aiData.type ?: "expense",
                category = aiData.category ?: "Other",
                description = aiData.description ?: "AI Added",
                date = Timestamp.now(),
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR)
            )
            transactionRepository.addTransaction(transaction).onSuccess { 
                _quickAddState.value = QuickAddState.Success("Added ${transaction.amount}")
                loadAllTransactions() 
            }
        }
    }

    // --- DATA LOADING & CRUD ---
    fun loadAllTransactions() {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getAllTransactions()
                .catch { e -> _transactionsState.value = TransactionsState.Error(e.message ?: "Error") }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    _transactionsState.value = TransactionsState.Success(transactions)
                    applySearchFilter()
                }
        }
    }

    fun loadTransactionsByType(type: String) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getTransactionsByType(type).collectLatest { 
                _cachedTransactions = it
                _transactionsState.value = TransactionsState.Success(it)
                applySearchFilter() 
            }
        }
    }

    fun loadTransactionsByMonthCompatible(month: Int, year: Int, type: String = "all") {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getAllTransactions().collectLatest { all ->
                val filtered = all.filter { tx ->
                    val cal = Calendar.getInstance().apply { time = tx.date.toDate() }
                    (cal.get(Calendar.MONTH) + 1) == month && cal.get(Calendar.YEAR) == year && (if (type == "all") true else tx.type == type)
                }
                _cachedTransactions = filtered
                _transactionsState.value = TransactionsState.Success(filtered)
                applySearchFilter()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            _transactionsState.value = TransactionsState.Success(_cachedTransactions)
        } else {
            val filtered = _cachedTransactions.filter { it.category.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
            _transactionsState.value = TransactionsState.Success(filtered)
        }
    }

    fun getTransactionById(id: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(id).fold(onSuccess = { _currentTransaction.value = it }, onFailure = {})
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch { transactionRepository.addTransaction(transaction).onSuccess { loadAllTransactions() } }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch { transactionRepository.updateTransaction(transaction).onSuccess { loadAllTransactions() } }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { transactionRepository.deleteTransaction(id).onSuccess { loadAllTransactions() } }
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) _selectedTransactionIds.value = emptySet()
    }

    fun toggleTransactionSelection(id: String) {
        _selectedTransactionIds.value = if (_selectedTransactionIds.value.contains(id)) _selectedTransactionIds.value - id else _selectedTransactionIds.value + id
    }

    fun selectAllTransaction(transactions: List<Transaction>) { _selectedTransactionIds.value = transactions.map { it.id }.toSet() }
    fun clearSelection() { _selectedTransactionIds.value = emptySet() }

    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            _selectedTransactionIds.value.forEach { transactionRepository.deleteTransaction(it) }
            _isSelectionMode.value = false
            _selectedTransactionIds.value = emptySet()
            loadAllTransactions()
        }
    }

    sealed interface QuickAddState {
        data object Idle : QuickAddState
        data object Loading : QuickAddState
        data class Success(val message: String) : QuickAddState
        data class Error(val message: String) : QuickAddState
    }

    sealed class TransactionsState {
        object Loading : TransactionsState()
        data class Success(val transactions: List<Transaction>) : TransactionsState()
        data class Error(val message: String) : TransactionsState()
    }
}
