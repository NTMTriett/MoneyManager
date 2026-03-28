package com.example.moneymanager.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onTransactionClick: (String) -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel()
) {
    val transactionsState by transactionViewModel.transactionsState.collectAsState(initial = TransactionViewModel.TransactionsState.Loading)
    val isSelectionMode by transactionViewModel.isSelectionMode.collectAsState(initial = false)
    val selectedTransactionIds by transactionViewModel.selectedTransactionIds.collectAsState(initial = emptySet())
    val searchQuery by transactionViewModel.searchQuery.collectAsState(initial = "")

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedTypeFilter by remember { mutableStateOf("all") } 
    var selectedMonthFilter by remember { mutableStateOf("All Time") }

    val months = listOf(
        "All Time", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
    }

    LaunchedEffect(selectedTypeFilter, selectedMonthFilter) {
        val monthIndex = months.indexOf(selectedMonthFilter)
        if (selectedMonthFilter == "All Time") {
            if (selectedTypeFilter == "all") transactionViewModel.loadAllTransactions()
            else transactionViewModel.loadTransactionsByType(selectedTypeFilter)
        } else {
            val calendar = Calendar.getInstance()
            transactionViewModel.loadTransactionsByMonthCompatible(monthIndex, calendar.get(Calendar.YEAR), selectedTypeFilter)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedTransactionIds.size,
                    totalCount = (transactionsState as? TransactionViewModel.TransactionsState.Success)?.transactions?.size ?: 0,
                    onClose = { transactionViewModel.toggleSelectionMode() },
                    onSelectAll = {
                        if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                            val transactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions
                            if (selectedTransactionIds.size == transactions.size) transactionViewModel.clearSelection()
                            else transactionViewModel.selectAllTransaction(transactions)
                        }
                    }
                )
            } else if (isSearchActive) {
                SearchTopAppBar(
                    query = searchQuery,
                    onQueryChange = { transactionViewModel.onSearchQueryChanged(it) },
                    onClose = {
                        isSearchActive = false
                        transactionViewModel.onSearchQueryChanged("")
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        floatingActionButton = {
            if (isSelectionMode) {
                FloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) { Icon(Icons.Default.Delete, "Delete") }
            } else {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(onClick = onNavigateToAddTransaction, containerColor = MediumGreen, contentColor = Color.White) {
                        Icon(Icons.Default.Add, "Add")
                    }
                    SmallFloatingActionButton(onClick = { transactionViewModel.toggleSelectionMode() }, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Icon(Icons.Default.Delete, "Select")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            if (!isSearchActive) {
                TransactionFilters(
                    selectedType = selectedTypeFilter,
                    onTypeSelected = { selectedTypeFilter = it },
                    selectedMonth = selectedMonthFilter,
                    onMonthSelected = { selectedMonthFilter = it },
                    months = months
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (val state = transactionsState) {
                is TransactionViewModel.TransactionsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center), color = MediumGreen) }
                }
                is TransactionViewModel.TransactionsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize()) { Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center)) }
                }
                is TransactionViewModel.TransactionsState.Success -> {
                    if (state.transactions.isEmpty()) {
                        EmptyStateView(isSearchActive)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.transactions, key = { it.id }) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedTransactionIds.contains(transaction.id),
                                    onTransactionClick = {
                                        if (isSelectionMode) transactionViewModel.toggleTransactionSelection(transaction.id)
                                        else onTransactionClick(transaction.id)
                                    },
                                    onTransactionLongClick = {
                                        if (!isSelectionMode) {
                                            transactionViewModel.toggleSelectionMode()
                                            transactionViewModel.toggleTransactionSelection(transaction.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            count = selectedTransactionIds.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { transactionViewModel.deleteSelectedTransactions(); showDeleteDialog = false }
        )
    }
}

@Composable
fun EmptyStateView(isSearchActive: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Receipt, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (isSearchActive) "No results found" else "No transactions yet", color = TextGray)
        }
    }
}

@Composable
fun DeleteConfirmDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Items") },
        text = { Text("Delete $count selected transactions?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = { OutlinedTextField(value = query, onValueChange = onQueryChange, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)) },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(selectedCount: Int, totalCount: Int, onClose: () -> Unit, onSelectAll: () -> Unit) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) } },
        actions = { IconButton(onClick = onSelectAll) { Icon(if (selectedCount == totalCount) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilters(selectedType: String, onTypeSelected: (String) -> Unit, selectedMonth: String, onMonthSelected: (String) -> Unit, months: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = selectedType == "all", onClick = { onTypeSelected("all") }, label = { Text("All") })
        FilterChip(selected = selectedType == "income", onClick = { onTypeSelected("income") }, label = { Text("Income") })
        FilterChip(selected = selectedType == "expense", onClick = { onTypeSelected("expense") }, label = { Text("Expense") })
        Spacer(Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            Row(modifier = Modifier.menuAnchor().clickable { expanded = true }.padding(8.dp)) {
                Text(selectedMonth, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                months.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { onMonthSelected(m); expanded = false }) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(transaction: Transaction, onTransactionClick: () -> Unit, isSelectionMode: Boolean, isSelected: Boolean, onTransactionLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onTransactionClick, onLongClick = onTransactionLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.5f) else Color.White),
        border = if (isSelected) BorderStroke(2.dp, MediumGreen) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onTransactionClick() })
                Spacer(Modifier.width(8.dp))
            }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (transaction.type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)), contentAlignment = Alignment.Center) {
                Icon(if (transaction.type == "income") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.Bold)
                Text(transaction.description, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextGray, fontSize = 12.sp)
            }
            Text(NumberFormat.getCurrencyInstance().format(transaction.amount), fontWeight = FontWeight.Black, color = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336))
        }
    }
}
