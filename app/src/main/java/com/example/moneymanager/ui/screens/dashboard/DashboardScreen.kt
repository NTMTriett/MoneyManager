package com.example.moneymanager.ui.screens.dashboard

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.ui.screens.transaction.ScanBillResultDialog
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.ui.viewmodel.BudgetViewModel
import com.example.moneymanager.ui.viewmodel.BudgetsUiState
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.io.File
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToDebtLoans: () -> Unit,
    onTransactionClick: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val quickAddState by transactionViewModel.quickAddState.collectAsState()
    val budgetsUiState by budgetViewModel.uiState.collectAsState()
    val categoriesState by categoryViewModel.categoriesState.collectAsState()
    var quickAddText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    var isBalanceVisible by remember { mutableStateOf(true) }
    var showScanDialog by remember { mutableStateOf(false) }
    var pendingCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher chọn ảnh từ thư viện
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            transactionViewModel.scanBill(bitmap)
        }
    }

    // Launcher chụp ảnh full-size từ Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraImageUri?.let { uri ->
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                transactionViewModel.scanBill(bitmap)
            }
        }
        pendingCameraImageUri = null
    }

    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
        budgetViewModel.loadBudgets()
        categoryViewModel.loadCategoriesByType("expense") // Pre-load categories for scan dialog
    }

    // Show snackbar feedback after quick-add or scan save
    LaunchedEffect(quickAddState) {
        when (val state = quickAddState) {
            is TransactionViewModel.QuickAddState.Success -> {
                quickAddText = ""
                snackbarHostState.showSnackbar(state.message)
            }
            is TransactionViewModel.QuickAddState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = MainBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.AutoAwesome, "AI Chat") }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item { NewDashboardHeader(user = currentUser, onProfileClick = onNavigateToProfile) }
            item {
                TotalBalanceCard(
                    transactionsState = transactionsState,
                    isVisible = isBalanceVisible,
                    onToggle = { isBalanceVisible = !isBalanceVisible },
                    onDetailClick = onNavigateToStatistics
                )
            }
            item {
                DashboardQuickAdd(
                    text = quickAddText,
                    onTextChanged = { quickAddText = it },
                    isLoading = quickAddState is TransactionViewModel.QuickAddState.Loading,
                    onSendClick = {
                        if (quickAddText.isNotBlank()) transactionViewModel.processQuickAdd(quickAddText)
                    }
                )
            }
            item {
                QuickActionsGrid(
                    onScanClick = { showScanDialog = true },
                    onAddClick = onNavigateToAddTransaction,
                    onHistoryClick = onNavigateToTransactions,
                    onBudgetClick = onNavigateToBudgets,
                    onCategoryClick = onNavigateToCategories,
                    onSavingsClick = onNavigateToSavings,
                    onNavigateToRecurring = onNavigateToRecurring,
                    onDebtLoanClick = onNavigateToDebtLoans
                )
            }
            item { RecentHeader(onSeeAllClick = onNavigateToTransactions) }

            when (val state = transactionsState) {
                is TransactionViewModel.TransactionsState.Success -> {
                    items(state.transactions.take(5)) { transaction ->
                        TransactionItemUI(transaction, onTransactionClick)
                    }
                }
                else -> {}
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // Scan Choice Dialog (Camera vs Gallery)
    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            title = { Text("Scan Bill", fontWeight = FontWeight.Bold) },
            text = { Text("Choose a method to scan your receipt.") },
            confirmButton = {
                Button(onClick = {
                    val imageFile = File.createTempFile("scan_bill_", ".jpg", context.cacheDir)
                    val imageUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        imageFile
                    )
                    pendingCameraImageUri = imageUri
                    cameraLauncher.launch(imageUri)
                    showScanDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)) {
                    Text("Take Photo")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { galleryLauncher.launch("image/*"); showScanDialog = false }) {
                    Text("Gallery", color = MediumGreen)
                }
            }
        )
    }

    // Scan Result Preview Dialog (Option B) — shown after AI processes the bill
    if (quickAddState is TransactionViewModel.QuickAddState.ScanResult) {
        val scanData = (quickAddState as TransactionViewModel.QuickAddState.ScanResult).data

        val categories = when (val cs = categoriesState) {
            is CategoryViewModel.CategoriesState.Success -> cs.categories
            else -> emptyList()
        }
        val activeBudgets = when (val bs = budgetsUiState) {
            is BudgetsUiState.Success -> bs.budgets
            else -> emptyList()
        }

        ScanBillResultDialog(
            scanData = scanData,
            categories = categories,
            activeBudgets = activeBudgets,
            onConfirm = { editedData ->
                transactionViewModel.confirmScanResult(editedData)
            },
            onDismiss = {
                transactionViewModel.resetQuickAddState()
            }
        )
    }
}

@Composable
fun NewDashboardHeader(user: com.example.moneymanager.data.model.User?, onProfileClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp)
            .background(Brush.horizontalGradient(listOf(MediumGreen, DarkGreen)))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(50.dp).clickable { onProfileClick() },
                shape = CircleShape, color = Color.White.copy(alpha = 0.2f)
            ) {
                if (!user?.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user?.photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hello!", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Text(user?.displayName ?: "Guest", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null, tint = Color.White) }
        }
    }
}

@Composable
fun TotalBalanceCard(transactionsState: TransactionViewModel.TransactionsState, isVisible: Boolean, onToggle: () -> Unit, onDetailClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-40).dp).shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total balance", color = TextGray, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).clickable { onToggle() },
                    tint = TextGray
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(14.dp).clickable { onDetailClick() }, tint = TextGray)
            }
            
            val balance = if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                transactionsState.transactions.filter { it.type == "income" }.sumOf { it.amount } - 
                transactionsState.transactions.filter { it.type == "expense" }.sumOf { it.amount }
            } else 0.0

            Text(
                text = if (isVisible) NumberFormat.getCurrencyInstance(Locale.getDefault()).format(balance) else "•••••••",
                fontSize = 30.sp, fontWeight = FontWeight.Black, color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = BackgroundGray)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val inc = if (transactionsState is TransactionViewModel.TransactionsState.Success) transactionsState.transactions.filter { it.type == "income" }.sumOf { it.amount } else 0.0
                val exp = if (transactionsState is TransactionViewModel.TransactionsState.Success) transactionsState.transactions.filter { it.type == "expense" }.sumOf { it.amount } else 0.0
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Text(" ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(inc)}", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    Text(" ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(exp)}", color = Color(0xFFD32F2F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DashboardQuickAdd(text: String, onTextChanged: (String) -> Unit, isLoading: Boolean, onSendClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-20).dp).shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BackgroundGray.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = MediumGreen, modifier = Modifier.padding(start = 8.dp).size(20.dp))
            OutlinedTextField(
                value = text, onValueChange = onTextChanged,
                placeholder = { Text("Ask AI to record (e.g. Lunch 5$)", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                enabled = !isLoading
            )
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MediumGreen)
            else IconButton(onClick = onSendClick) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = MediumGreen) }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onScanClick: () -> Unit,
    onAddClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onSavingsClick: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onDebtLoanClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActionIcon("Scan", Icons.Default.DocumentScanner, Color(0xFF1A73E8), onScanClick)
            ActionIcon("Add", Icons.Default.AddCircle, Color(0xFFE91E63), onAddClick)
            ActionIcon("History", Icons.Default.History, Color(0xFFF57C00), onHistoryClick)
            ActionIcon("Budgets", Icons.Default.PieChart, Color(0xFF673AB7), onBudgetClick)
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActionIcon("Savings", Icons.Default.AccountBalanceWallet, Color(0xFF43A047), onSavingsClick)
            ActionIcon("Recurring", Icons.Default.Repeat, Color(0xFF0288D1), onNavigateToRecurring)
            ActionIcon("Debt/Loan", Icons.Default.MoneyOff, Color(0xFFE64A19), onDebtLoanClick)
            ActionIcon("Category", Icons.Default.GridView, Color(0xFF00BFA5), onCategoryClick)
        }
    }
}

@Composable
fun ActionIcon(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(75.dp).clickable { onClick() }) {
        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun RecentHeader(onSeeAllClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Recent Transactions", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("See All", color = MainBlue, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSeeAllClick() })
    }
}

@Composable
fun TransactionItemUI(transaction: Transaction, onClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).clickable { onClick(transaction.id) }, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (transaction.type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)), contentAlignment = Alignment.Center) {
            Icon(if (transaction.type == "income") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown, null, 
                tint = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.category, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(transaction.date.toDate()), fontSize = 12.sp, color = TextGray)
        }
        Text(
            (if (transaction.type == "income") "+" else "-") + NumberFormat.getCurrencyInstance().format(transaction.amount),
            fontWeight = FontWeight.Black, color = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}
