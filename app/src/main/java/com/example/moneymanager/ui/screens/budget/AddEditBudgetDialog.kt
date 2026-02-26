package com.example.moneymanager.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Budget
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    budget: Budget?,
    onDismiss: () -> Unit,
    onConfirm: (Budget) -> Unit,
    onDelete: () -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categoriesState by categoryViewModel.categoriesState.collectAsState()
    LaunchedEffect(Unit) {
        categoryViewModel.loadCategoriesByType("expense")
    }

    var selectedCategory by remember { mutableStateOf(budget?.category ?: "") }
    var amount by remember { mutableStateOf(budget?.allocatedAmount?.toString() ?: "") }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = budget?.startDate?.time,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "Add Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded && budget == null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = budget == null // Disable editing category for existing budget
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        if (categoriesState is CategoryViewModel.CategoriesState.Success) {
                            (categoriesState as CategoryViewModel.CategoriesState.Success).categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category.name
                                        isCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Allocated Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = datePickerState.selectedDateMillis?.let { formatDate(it) } ?: "Select Month",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Month") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val allocated = amount.toDoubleOrNull()
                val selectedDate = datePickerState.selectedDateMillis
                if (selectedCategory.isNotBlank() && allocated != null && allocated > 0 && selectedDate != null) {
                    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val startDate = calendar.time
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    val endDate = calendar.time

                    val newBudget = budget?.copy(
                        allocatedAmount = allocated,
                        startDate = startDate,
                        endDate = endDate
                    ) ?: Budget(
                        category = selectedCategory,
                        allocatedAmount = allocated,
                        startDate = startDate,
                        endDate = endDate
                    )
                    onConfirm(newBudget)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { 
                TextButton(onClick = { showDatePicker = false }) { Text("OK") } 
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (budget != null) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete Budget")
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
}
