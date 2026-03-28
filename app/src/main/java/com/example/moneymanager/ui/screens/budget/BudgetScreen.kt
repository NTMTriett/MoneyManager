package com.example.moneymanager.ui.screens.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Budget
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.ui.viewmodel.BudgetViewModel
import com.example.moneymanager.ui.viewmodel.BudgetsUiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedBudget by remember { mutableStateOf<Budget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Help */ }) { Icon(Icons.Default.Info, null, tint = TextGray) }
                    IconButton(onClick = { /* Sort */ }) { Icon(Icons.Default.SwapVert, null, tint = TextGray) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedBudget = null
                    showAddEditDialog = true 
                },
                containerColor = MainBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF8F9FA))) {
            when (val state = uiState) {
                is BudgetsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MainBlue)
                    }
                }
                is BudgetsUiState.Success -> {
                    if (state.budgets.isEmpty()) {
                        EmptyBudgetState { 
                            selectedBudget = null
                            showAddEditDialog = true 
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { OverallSummaryCard(state) }
                            items(state.budgets) { budget ->
                                BudgetItemCard(budget) {
                                    selectedBudget = budget
                                    showAddEditDialog = true
                                }
                            }
                        }
                    }
                }
                is BudgetsUiState.Error -> {
                    Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditBudgetDialog(
            budget = selectedBudget,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { budget ->
                if (selectedBudget == null) {
                    viewModel.saveBudget(budget)
                } else {
                    viewModel.updateBudget(budget)
                }
                showAddEditDialog = false
            },
            onDelete = {
                selectedBudget?.id?.let { viewModel.deleteBudget(it) }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun EmptyBudgetState(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(BackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.HelpOutline, null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("You have no budget!", fontWeight = FontWeight.Medium, color = TextGray)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MainBlue)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add budget", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { }) {
                Text("What is budget?", color = MainBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OverallSummaryCard(state: BudgetsUiState.Success) {
    val progress = if (state.overallBudget > 0) (state.overallSpent / state.overallBudget).toFloat() else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = 1f, modifier = Modifier.fillMaxSize(),
                    color = BackgroundGray, strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize(),
                    color = MainBlue, strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Text("${(progress * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Overall Spent", fontSize = 14.sp, color = TextGray)
                Text(formatCurrency(state.overallSpent), fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text("of ${formatCurrency(state.overallBudget)}", fontSize = 12.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun BudgetItemCard(budget: Budget, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MainBlue.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Text(budget.category.take(1), color = MainBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${formatDate(budget.startDate)} - ${formatDate(budget.endDate)}", fontSize = 12.sp, color = TextGray)
                }
                Text(formatCurrency(budget.allocatedAmount), fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(BackgroundGray)) {
                Box(modifier = Modifier.fillMaxWidth(budget.progress.coerceIn(0f, 1f)).fillMaxHeight().background(MainBlue))
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ongoing", fontSize = 11.sp, color = TextGray)
                Text("Remaining ${formatCurrency(budget.allocatedAmount - budget.spentAmount)}", fontSize = 11.sp, color = TextGray)
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
}
