package com.example.moneymanager.ui.screens.debtloan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.* 
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.DebtLoan
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.ui.viewmodel.DebtLoanViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtLoansScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtLoanViewModel = hiltViewModel()
) {
    val debtLoans by viewModel.debtLoansState.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<DebtLoan?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var processingId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("Active", "Completed")
    val filteredLoans = remember(debtLoans, selectedTab) {
        if (selectedTab == 0) debtLoans.filter { !it.isResolved }
        else debtLoans.filter { it.isResolved }
    }

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
            FloatingActionButton(
                onClick = { 
                    selectedItem = null
                    showAddEditDialog = true 
                },
                containerColor = MediumGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Summary Card
            Box(modifier = Modifier.padding(16.dp)) {
                DebtSummaryCard(debtLoans)
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MediumGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MediumGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (filteredLoans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records in this category.", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLoans, key = { it.id }) { item ->
                        DebtLoanItem(
                            item = item,
                            isProcessing = processingId == item.id,
                            onPayClick = {
                                selectedItem = item
                                showPaymentDialog = true
                            },
                            onResolve = { 
                                processingId = item.id
                                viewModel.resolveDebtLoan(item) 
                            },
                            onDelete = { viewModel.deleteDebtLoan(item.id) },
                            onClick = {
                                if (!item.isResolved) {
                                    selectedItem = item
                                    showAddEditDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditDebtLoanDialog(
            existingItem = selectedItem,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { debtLoan ->
                if (selectedItem == null) {
                    viewModel.addDebtLoan(debtLoan)
                } else {
                    viewModel.updateDebtLoan(debtLoan)
                }
                showAddEditDialog = false
            }
        )
    }

    if (showPaymentDialog && selectedItem != null) {
        PaymentDialog(
            debtLoan = selectedItem!!,
            onDismiss = { 
                showPaymentDialog = false
                selectedItem = null
            },
            onConfirm = { amount ->
                viewModel.payDebtLoan(selectedItem!!, amount)
                showPaymentDialog = false
                selectedItem = null
            }
        )
    }
}

@Composable
fun DebtSummaryCard(items: List<DebtLoan>) {
    val totalDebt = items.filter { it.type == "debt" && !it.isResolved }.sumOf { it.remainingAmount }
    val totalLoan = items.filter { it.type == "loan" && !it.isResolved }.sumOf { it.remainingAmount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("They owe you", fontSize = 12.sp, color = TextGray)
                Text(
                    NumberFormat.getCurrencyInstance().format(totalLoan),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You owe them", fontSize = 12.sp, color = TextGray)
                Text(
                    NumberFormat.getCurrencyInstance().format(totalDebt),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun DebtLoanItem(
    item: DebtLoan, 
    isProcessing: Boolean,
    onPayClick: () -> Unit,
    onResolve: () -> Unit, 
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.isResolved) Color(0xFFF5F5F5) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
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
                    Text(item.personName, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (item.type == "debt") "Borrowed" else "Lent", fontSize = 12.sp, color = TextGray)
                    
                    item.dueDate?.let {
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate())
                        val isOverdue = it.toDate().before(Date()) && !item.isResolved
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, modifier = Modifier.size(12.dp), tint = if (isOverdue) Color.Red else TextGray)
                            Spacer(Modifier.width(4.dp))
                            Text("Due: $dateStr", fontSize = 11.sp, color = if (isOverdue) Color.Red else TextGray, fontWeight = if(isOverdue) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        NumberFormat.getCurrencyInstance().format(item.amount),
                        fontWeight = FontWeight.Black,
                        color = if (item.type == "debt") Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                    if (item.paidAmount > 0 && !item.isResolved) {
                        Text(
                            "Paid: ${NumberFormat.getCurrencyInstance().format(item.paidAmount)}",
                            fontSize = 11.sp,
                            color = MediumGreen
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }

            if (!item.isResolved) {
                Spacer(modifier = Modifier.height(12.dp))
                val progressValue = (item.paidAmount / item.amount).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = if (item.type == "debt") Color(0xFFF44336) else Color(0xFF4CAF50),
                    trackColor = BackgroundGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MediumGreen)
                    } else {
                        TextButton(onClick = onPayClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Pay", fontSize = 13.sp, color = MediumGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onResolve, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Settle Full", fontSize = 13.sp, color = MediumGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtLoanDialog(
    existingItem: DebtLoan?,
    onDismiss: () -> Unit, 
    onConfirm: (DebtLoan) -> Unit
) {
    var personName by remember { mutableStateOf(existingItem?.personName ?: "") }
    var amount by remember { mutableStateOf(existingItem?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(existingItem?.type ?: "debt") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(existingItem?.dueDate?.toDate()?.time) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem == null) "New Debt/Loan" else "Edit Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existingItem == null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "debt",
                            onClick = { type = "debt" },
                            label = { Text("I borrowed") }
                        )
                        FilterChip(
                            selected = type == "loan",
                            onClick = { type = "loan" },
                            label = { Text("I lent") }
                        )
                    }
                }

                OutlinedTextField(
                    value = personName, 
                    onValueChange = { personName = it }, 
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Total Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = selectedDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "",
                    onValueChange = {},
                    label = { Text("Due Date (Optional)") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm((existingItem ?: DebtLoan()).copy(
                        personName = personName,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        // Do not allow updating paidAmount directly from this dialog
                        // paidAmount = existingItem?.paidAmount ?: 0.0,
                        type = type, // Type should ideally be immutable for existing items
                        description = description,
                        dueDate = selectedDate?.let { Timestamp(Date(it)) }
                    ))
                },
                enabled = personName.isNotBlank() && amount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun PaymentDialog(
    debtLoan: DebtLoan,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val remaining = debtLoan.remainingAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Remaining to pay: ${NumberFormat.getCurrencyInstance().format(remaining)}",
                    fontSize = 14.sp,
                    color = TextGray
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Payment Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payAmt = amount.toDoubleOrNull()
                    if (payAmt != null && payAmt > 0 && payAmt <= remaining) {
                        onConfirm(payAmt)
                    } else {
                         // Show an error to the user if amount is invalid or exceeds remaining
                    }
                },
                enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && (amount.toDoubleOrNull() ?: 0.0) <= remaining,
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)
            ) { Text("Record") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
