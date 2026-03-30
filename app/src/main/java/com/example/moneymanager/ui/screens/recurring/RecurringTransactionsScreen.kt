package com.example.moneymanager.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.RecurringTransaction
import com.example.moneymanager.data.model.Category
import com.example.moneymanager.ui.screens.category.CategorySelector
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.viewmodel.RecurringTransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val recurringTransactions by viewModel.recurringState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Hiển thị thông báo thành công hoặc lỗi
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recurring Payments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (!showAddDialog) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MediumGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Recurring")
                }
            }
        }
    ) { paddingValues ->
        if (recurringTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No recurring transactions yet.", color = TextGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recurringTransactions) { item ->
                    RecurringItem(
                        item = item,
                        onDelete = { viewModel.deleteRecurring(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { transaction ->
                viewModel.addRecurring(transaction)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RecurringItem(item: RecurringTransaction, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Repeat, null, tint = MediumGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.category, fontWeight = FontWeight.Bold)
                Text(item.description, fontSize = 12.sp, color = TextGray)
                Text("Frequency: ${item.frequency.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = MediumGreen)
            }
            Text(
                NumberFormat.getCurrencyInstance().format(item.amount),
                fontWeight = FontWeight.Black,
                color = if (item.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun AddRecurringDialog(onDismiss: () -> Unit, onConfirm: (RecurringTransaction) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var desc by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") }
    var frequency by remember { mutableStateOf("monthly") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Recurring Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = transactionType == "expense",
                        onClick = { transactionType = "expense" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = transactionType == "income",
                        onClick = { transactionType = "income" },
                        label = { Text("Income") }
                    )
                }

                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                CategorySelector(
                    selectedCategory = selectedCategory,
                    transactionType = transactionType,
                    onCategorySelected = { selectedCategory = it }
                )

                OutlinedTextField(
                    value = desc, 
                    onValueChange = { desc = it }, 
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Frequency: ${frequency.replaceFirstChar { it.uppercase() }}")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("daily", "weekly", "monthly", "yearly").forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f.replaceFirstChar { it.uppercase() }) },
                                onClick = { frequency = f; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(RecurringTransaction(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        type = transactionType,
                        category = selectedCategory?.name ?: "Other",
                        description = desc,
                        frequency = frequency
                    ))
                },
                enabled = amount.isNotBlank() && selectedCategory != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
