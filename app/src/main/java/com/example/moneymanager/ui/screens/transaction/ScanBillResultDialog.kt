package com.example.moneymanager.ui.screens.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneymanager.data.model.AiTransactionData
import com.example.moneymanager.data.model.Budget
import com.example.moneymanager.data.model.Category
import com.example.moneymanager.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Dialog shown after AI scans a bill (Option B: Preview before saving).
 * Lets the user verify / edit the category before the transaction is persisted.
 *
 * @param scanData       The AI-extracted transaction data.
 * @param categories     All user categories (for the editable dropdown).
 * @param activeBudgets  Currently active budgets (to show which budget will be impacted).
 * @param onConfirm      Called with (possibly edited) [AiTransactionData] when user taps "Save".
 * @param onDismiss      Called when user taps "Cancel" or outside the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBillResultDialog(
    scanData: AiTransactionData,
    categories: List<Category>,
    activeBudgets: List<Budget>,
    onConfirm: (AiTransactionData) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryName by remember { mutableStateOf(scanData.category ?: "Other") }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val impactedBudget = activeBudgets.firstOrNull {
        it.category.equals(selectedCategoryName, ignoreCase = true)
    }

    val isExpense = (scanData.type ?: "expense") == "expense"
    val typeColor = if (isExpense) ErrorRed else MainGreen
    val typeLabel = if (isExpense) "Expense" else "Income"
    val typeIcon = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val currency = scanData.currency ?: "VND"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // Scrollable so the expanded category list doesn't overflow
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    // ── Header ──────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MainBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MainBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Scan Result",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Review and edit before saving",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = DividerGray)
                    Spacer(Modifier.height(20.dp))

                    // Amount + Currency badge + Type chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = formatAmount(scanData.amount ?: 0.0, currency),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = typeColor
                            )
                            // Show currency badge when not VND so user is aware
                            if (currency != "VND") {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = WarningOrange.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = currency,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningOrange,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = typeColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    typeIcon,
                                    null,
                                    tint = typeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    typeLabel,
                                    color = typeColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Description ──────────────────────────────────────
                    if (!scanData.description.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = scanData.description,
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Category selector — inline expandable dropdown (no z-order issues)
                    Text(
                        "Category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                    Spacer(Modifier.height(6.dp))

                    // Selector row (tap to toggle list)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryPicker = !showCategoryPicker }
                            .border(1.5.dp, MainBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = LightBlue.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MainBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedCategoryName.take(1).uppercase(),
                                        color = MainBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    selectedCategoryName,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = if (showCategoryPicker)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle categories",
                                tint = MainBlue
                            )
                        }
                    }

                    // Inline category list — expands below the selector row
                    AnimatedVisibility(
                        visible = showCategoryPicker && categories.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
                                .heightIn(max = 220.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                categories.forEachIndexed { index, category ->
                                    val isSelected = category.name.equals(
                                        selectedCategoryName,
                                        ignoreCase = true
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategoryName = category.name
                                                showCategoryPicker = false
                                            }
                                            .background(
                                                if (isSelected) LightBlue.copy(alpha = 0.5f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MainBlue.copy(0.15f) else BackgroundGray
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = category.name.take(1).uppercase(),
                                                color = if (isSelected) MainBlue else TextGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = category.name,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MainBlue else TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = MainBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (index < categories.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            color = DividerGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Budget impact banner ─────────────────────────────
                    AnimatedVisibility(visible = impactedBudget != null) {
                        impactedBudget?.let { budget ->
                            Spacer(Modifier.height(12.dp))
                            val remaining = budget.allocatedAmount - budget.spentAmount
                            val afterSpend = remaining - (scanData.amount ?: 0.0)
                            val isOverBudget = afterSpend < 0
                            val bannerColor = if (isOverBudget) LightRed else LightGreen
                            val bannerBorder = if (isOverBudget) ErrorRed else MainGreen
                            val bannerIcon =
                                if (isOverBudget) Icons.Default.Warning else Icons.Default.CheckCircle

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = bannerColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        bannerBorder.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = bannerIcon,
                                        contentDescription = null,
                                        tint = bannerBorder,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Budget: ${budget.category}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = bannerBorder
                                        )
                                        Text(
                                            text = if (isOverBudget)
                                                "Over budget! Remaining ${
                                                    formatAmount(
                                                        remaining,
                                                        currency
                                                    )
                                                }, will exceed by ${
                                                    formatAmount(
                                                        -afterSpend,
                                                        currency
                                                    )
                                                }"
                                            else
                                                "Remaining after save: ${
                                                    formatAmount(
                                                        afterSpend,
                                                        currency
                                                    )
                                                } / ${
                                                    formatAmount(
                                                        budget.allocatedAmount,
                                                        currency
                                                    )
                                                }",
                                            fontSize = 12.sp,
                                            color = bannerBorder.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onConfirm(scanData.copy(category = selectedCategoryName)) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MainBlue)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Currency-aware amount formatter.
 * - VND: no decimals, Vietnamese thousands format  → 350.000đ
 * - USD: 2 decimals with $ prefix                 → $6.33
 * - EUR: 2 decimals with € prefix                 → €12.50
 * - Others: 2 decimals + currency code             → 45.50 GBP
 */
internal fun formatAmount(amount: Double, currency: String): String {
    return when (currency.uppercase()) {
        "VND" -> {
            val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            fmt.maximumFractionDigits = 0
            "${fmt.format(amount.toLong())}đ"
        }
        "USD" -> "\$${String.format("%.2f", amount)}"
        "EUR" -> "€${String.format("%.2f", amount)}"
        "GBP" -> "£${String.format("%.2f", amount)}"
        "JPY", "CNY" -> "${String.format("%.0f", amount)} $currency"
        else -> "${String.format("%.2f", amount)} $currency"
    }
}
