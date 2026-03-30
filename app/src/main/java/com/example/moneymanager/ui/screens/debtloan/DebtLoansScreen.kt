package com.example.moneymanager.ui.screens.debtloan

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.DebtLoan
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.DebtLoanViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtLoansScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtLoanViewModel = hiltViewModel()
) {
    val debtLoans by viewModel.debtLoansState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Quản lý ID đang được xử lý để hiện vòng quay loading
    var processingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            processingId = null
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Debts & Loans", fontWeight = FontWeight.Bold) },
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
                    Icon(Icons.Default.Add, "Add Entry")
                }
            }
        }
    ) { paddingValues ->
        if (debtLoans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No records yet.", color = TextGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(debtLoans, key = { it.id }) { item ->
                    DebtLoanItem(
                        item = item,
                        isProcessing = processingId == item.id,
                        onResolve = { 
                            processingId = item.id
                            viewModel.resolveDebtLoan(item) 
                        },
                        onDelete = { viewModel.deleteDebtLoan(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtLoanDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { debtLoan ->
                viewModel.addDebtLoan(debtLoan)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DebtLoanItem(
    item: DebtLoan, 
    isProcessing: Boolean,
    onResolve: () -> Unit, 
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.isResolved) Color(0xFFF5F5F5) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (item.type == "debt") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == "debt") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (item.type == "debt") Color(0xFFF44336) else Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.personName, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (item.isResolved) {
                        Text(" (Complete)", fontSize = 12.sp, color = MediumGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Text(if (item.type == "debt") "You borrowed" else "You lent", fontSize = 12.sp, color = TextGray)
                if (item.description.isNotBlank()) {
                    Text(item.description, fontSize = 11.sp, color = TextGray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    NumberFormat.getCurrencyInstance().format(item.amount),
                    fontWeight = FontWeight.Black,
                    color = if (item.type == "debt") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                
                // Nút chỉ hiện khi CHƯA Resolve
                if (!item.isResolved) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MediumGreen)
                    } else {
                        TextButton(onClick = onResolve, contentPadding = PaddingValues(0.dp)) {
                            Text("Mark Resolved", fontSize = 11.sp, color = MediumGreen)
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddDebtLoanDialog(onDismiss: () -> Unit, onConfirm: (DebtLoan) -> Unit) {
    var personName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("debt") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Debt/Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "debt",
                        onClick = { type = "debt" },
                        label = { Text("Borrowed (Debt)") }
                    )
                    FilterChip(
                        selected = type == "loan",
                        onClick = { type = "loan" },
                        label = { Text("Lent (Loan)") }
                    )
                }

                OutlinedTextField(
                    value = personName, 
                    onValueChange = { personName = it }, 
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(DebtLoan(
                        personName = personName,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        type = type,
                        description = description
                    ))
                },
                enabled = personName.isNotBlank() && amount.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
