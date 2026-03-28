package com.example.moneymanager.ui.screens.transaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Category
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.ui.screens.category.CategorySelector
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray // Explicit import
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: String? = null,
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Transaction state
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") } // Default to expense
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // Data Observables - Explicit initial values to fix type inference
    val categoriesState by categoryViewModel.categoriesState.collectAsState(initial = CategoryViewModel.CategoriesState.Loading)
    val transaction by transactionViewModel.currentTransaction.collectAsState(initial = null)
    val transactionsState by transactionViewModel.transactionsState.collectAsState(initial = TransactionViewModel.TransactionsState.Loading)

    // Date Helpers
    val calendar = Calendar.getInstance().apply { time = selectedDate }
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )
    var showDatePicker by remember { mutableStateOf(false) }

    // Dynamic Colors based on Type
    // Income = Emerald (MediumGreen), Expense = Ruby (Red)
    val primaryColor by animateColorAsState(
        targetValue = if (transactionType == "income") MediumGreen else Color(0xFFD32F2F),
        animationSpec = tween(300), label = "primaryColor"
    )

    // --- Logic Handlers ---

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Date(millis)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(transactionType) {
        categoryViewModel.loadCategoriesByType(transactionType)
        if (transactionId == null) {
            selectedCategory = null
        }
    }

    LaunchedEffect(categoriesState) {
        if (categoriesState is CategoryViewModel.CategoriesState.Success) {
            val categories = (categoriesState as CategoryViewModel.CategoriesState.Success).categories
            if (categories.isEmpty()) {
                categoryViewModel.createDefaultCategories()
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            transactionViewModel.getTransactionById(transactionId)
        }
    }

    LaunchedEffect(transaction) {
        transaction?.let {
            amount = it.amount.toString()
            description = it.description
            transactionType = it.type
            selectedCategory = categoriesState.let { state ->
                if (state is CategoryViewModel.CategoriesState.Success) {
                    state.categories.find { category -> category.name == it.category }
                } else null
            }
            selectedDate = it.date.toDate()
        }
    }

    // --- UI Content ---

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (transactionId == null) "Add Transaction" else "Edit Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Type Switcher (Emerald vs Ruby)
            TransactionTypeSwitcher(
                currentType = transactionType,
                onTypeSelected = { transactionType = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // 2. Amount Input (Big & Central)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Enter Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = {
                            Text(
                                "0.00",
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = primaryColor
                        )
                    )
                }

                // 3. Category Selector
                CategorySelector(
                    selectedCategory = selectedCategory,
                    transactionType = transactionType,
                    onCategorySelected = { category -> selectedCategory = category }
                )

                // 4. Date Picker
                ClickableTextField(
                    value = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(selectedDate),
                    label = "Date",
                    icon = Icons.Default.CalendarToday,
                    primaryColor = primaryColor,
                    onClick = { showDatePicker = true }
                )

                // 5. Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = primaryColor)
                    },
                    minLines = 3,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Save Button
                Button(
                    onClick = {
                        if (amount.isNotBlank()) {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue == null || amountValue <= 0) return@Button

                            val categoryName = selectedCategory?.name?.takeIf { it.isNotBlank() } ?: "Other"

                            val transactionObj = Transaction(
                                id = transactionId ?: "",
                                amount = amountValue,
                                type = transactionType,
                                category = categoryName,
                                description = description.trim(),
                                date = Timestamp(selectedDate),
                                month = month,
                                year = year
                            )

                            if (transactionId == null) {
                                transactionViewModel.addTransaction(transactionObj)
                            } else {
                                transactionViewModel.updateTransaction(transactionObj)
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    )
                ) {
                    Text(
                        if (transactionId == null) "Save Transaction" else "Update Transaction",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TransactionTypeSwitcher(
    currentType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color.White.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val incomeSelected = currentType == "income"
        val expenseSelected = currentType == "expense"

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(if (expenseSelected) Color(0xFFD32F2F) else Color.Transparent)
                .clickable { onTypeSelected("expense") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Expense",
                color = if (expenseSelected) Color.White else TextGray,
                fontWeight = if (expenseSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(if (incomeSelected) MediumGreen else Color.Transparent)
                .clickable { onTypeSelected("income") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Income",
                color = if (incomeSelected) Color.White else TextGray,
                fontWeight = if (incomeSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ClickableTextField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = primaryColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

private val TextPrimary = Color(0xFF212121)
