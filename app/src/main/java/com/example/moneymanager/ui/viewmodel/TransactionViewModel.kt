package com.example.moneymanager.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
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
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val ollamaRepository: OllamaRepository
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

    // Trạng thái lưu dữ liệu AI quét được để người dùng xác nhận
    private val _scannedTransaction = MutableStateFlow<AiTransactionData?>(null)
    val scannedTransaction: StateFlow<AiTransactionData?> = _scannedTransaction.asStateFlow()

    private var _cachedTransactions: List<Transaction> = emptyList()
    private val gson = Gson()
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    init {
        loadAllTransactions()
    }

    fun loadAllTransactions() {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getAllTransactions()
                .catch { e -> 
                    Log.e("TransactionVM", "Error loading: ${e.message}")
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Error") 
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    filterTransactions()
                }
        }
    }

    fun loadTransactionsByType(type: String) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getTransactionsByType(type)
                .catch { e -> _transactionsState.value = TransactionsState.Error(e.message ?: "Error") }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    filterTransactions()
                }
        }
    }

    fun loadTransactionsByMonthCompatible(month: Int, year: Int, type: String) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getTransactionsByMonth(month, year)
                .catch { e -> _transactionsState.value = TransactionsState.Error(e.message ?: "Error") }
                .collectLatest { transactions ->
                    _cachedTransactions = if (type == "all") transactions else transactions.filter { it.type == type }
                    filterTransactions()
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterTransactions()
    }

    private fun filterTransactions() {
        val query = _searchQuery.value.lowercase()
        val filtered = if (query.isEmpty()) {
            _cachedTransactions
        } else {
            _cachedTransactions.filter { 
                it.category.lowercase().contains(query) || 
                it.description.lowercase().contains(query) ||
                it.amount.toString().contains(query)
            }
        }
        _transactionsState.value = TransactionsState.Success(filtered)
    }

    fun getTransactionById(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(transactionId).onSuccess {
                _currentTransaction.value = it
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.addTransaction(transaction).onSuccess {
                loadAllTransactions()
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction).onSuccess {
                loadAllTransactions()
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId).onSuccess {
                loadAllTransactions()
            }
        }
    }

    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            val idsToDelete = _selectedTransactionIds.value
            idsToDelete.forEach { id ->
                transactionRepository.deleteTransaction(id)
            }
            _selectedTransactionIds.value = emptySet()
            _isSelectionMode.value = false
            loadAllTransactions()
        }
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun toggleTransactionSelection(transactionId: String) {
        val currentSelected = _selectedTransactionIds.value.toMutableSet()
        if (currentSelected.contains(transactionId)) {
            currentSelected.remove(transactionId)
        } else {
            currentSelected.add(transactionId)
        }
        _selectedTransactionIds.value = currentSelected
    }

    fun selectAllTransaction(transactions: List<Transaction>) {
        _selectedTransactionIds.value = transactions.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
    }

    fun scanBill(bitmap: Bitmap) {
        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading
            val prompt = """
            Analyze this receipt image and extract:
            1. amount (number)
            2. type (always 'expense')
            3. category (choose best from: Food, Transport, Shopping, Bills, Others)
            4. description (short summary of what was bought)
            Return ONLY a valid JSON object.
        """.trimIndent()

            try {
                val response = generativeModel.generateContent(content {
                    image(bitmap)
                    text(prompt)
                })

                val rawText = response.text ?: ""
                val startIndex = rawText.indexOf("{")
                val endIndex = rawText.lastIndexOf("}")

                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    val cleanJson = rawText.substring(startIndex, endIndex + 1)
                    val aiData = gson.fromJson(cleanJson, AiTransactionData::class.java)

                    _scannedTransaction.value = aiData
                    _quickAddState.value = QuickAddState.Idle
                } else {
                    _quickAddState.value = QuickAddState.Error("AI returned invalid format. Please try again.")
                }

            } catch (e: Exception) {
                Log.e("ScanBill", "Error: ${e.message}")
                _quickAddState.value = QuickAddState.Error("AI Scan failed: ${e.message}")
            }
        }
    }

    fun clearScannedTransaction() {
        _scannedTransaction.value = null
    }

    fun confirmAndSaveTransaction(amount: Double, category: String, description: String) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val transaction = Transaction(
                amount = amount,
                type = "expense",
                category = category,
                description = description,
                date = Timestamp.now(),
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR)
            )
            transactionRepository.addTransaction(transaction).onSuccess { 
                _quickAddState.value = QuickAddState.Success("Transaction saved!")
                _scannedTransaction.value = null
                loadAllTransactions() 
            }
        }
    }

    fun processQuickAdd(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading
            try {
                val result = ollamaRepository.sendMessage(input, modelName = "qwen2.5:3b", jsonMode = true)
                result.fold(
                    onSuccess = { jsonResponse ->
                        val cleanJson = jsonResponse.substringAfter("{").substringBeforeLast("}").let { "{ $it }" }
                        val aiData = gson.fromJson(cleanJson, AiTransactionData::class.java)
                        _scannedTransaction.value = aiData
                        _quickAddState.value = QuickAddState.Idle
                    },
                    onFailure = { _quickAddState.value = QuickAddState.Error("Ollama error") }
                )
            } catch (e: Exception) {
                _quickAddState.value = QuickAddState.Error(e.message ?: "Error")
            }
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
