package com.example.moneymanager.ui.screens.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val categoriesState by categoryViewModel.categoriesState.collectAsState()

    // State for filters
    var selectedPeriod by remember { mutableStateOf("Month") } // Week, Month, Year
    var selectedType by remember { mutableStateOf("expense") } // income, expense

    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
        categoryViewModel.loadAllCategories()
    }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        }
    ) { padding ->
        if (transactionsState is TransactionViewModel.TransactionsState.Loading ||
            categoriesState is CategoryViewModel.CategoriesState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MediumGreen)
            }
        } else if (transactionsState is TransactionViewModel.TransactionsState.Success &&
            categoriesState is CategoryViewModel.CategoriesState.Success) {

            val allTransactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions
            val filteredTransactions = filterTransactions(allTransactions, selectedPeriod, selectedType)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. Custom Segmented Controls
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Income/Expense Toggle
                        TypeSelector(selectedType) { selectedType = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Time Period Toggle
                        PeriodSelector(selectedPeriod) { selectedPeriod = it }
                    }
                }

                // 2. Main Donut Chart Section with Legend
                item {
                    if (filteredTransactions.isNotEmpty()) {
                        DonutChartSection(filteredTransactions, selectedType)
                    } else {
                        EmptyChartState()
                    }
                }

                // 3. Bar Chart Trend Section
                item {
                    if (filteredTransactions.isNotEmpty()) {
                        TrendChartSection(allTransactions, selectedPeriod, selectedType)
                    }
                }

                // 4. Category Breakdown List
                item {
                    if (filteredTransactions.isNotEmpty()) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                }

                if (filteredTransactions.isNotEmpty()) {
                    val categoryTotals = filteredTransactions
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }
                        .toList()
                        .sortedByDescending { it.second }

                    items(categoryTotals) { (catName, amount) ->
                        val total = filteredTransactions.sumOf { it.amount }
                        val percentage = (amount / total).toFloat()
                        CategoryBreakdownItem(catName, amount, percentage, selectedType)
                    }
                }
            }
        }
    }
}

// --- UI Components ---

@Composable
fun TypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val types = listOf("expense" to "Expense", "income" to "Income")
        types.forEach { (key, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == key)
                        if(key == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
                    else Color.Transparent)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = if (selected == key) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("Week", "Month", "Year").forEach { period ->
            val isSelected = selected == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) MediumGreen else Color.Transparent)
                    .clickable { onSelect(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
fun DonutChartSection(transactions: List<Transaction>, type: String) {
    val totalAmount = transactions.sumOf { it.amount }
    val categoryData = transactions.groupBy { it.category }
        .map { it.key to it.value.sumOf { tx -> tx.amount } }
        .sortedByDescending { it.second }
        .take(5) // Take top 5 for chart/legend

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Title centered
            Text(
                text = "Total ${type.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chart on the Left
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedDonutChart(
                        data = categoryData,
                        total = totalAmount,
                        modifier = Modifier.size(130.dp)
                    )
                    // Inner Text with custom compact formatter
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCompactNumber(totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Legend on the Right
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categoryData.forEachIndexed { index, (category, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getChartColor(index))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${(amount / totalAmount * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedDonutChart(
    data: List<Pair<String, Double>>,
    total: Double,
    modifier: Modifier
) {
    val animation = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animation.snapTo(0f)
        animation.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = 35f
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        data.forEachIndexed { index, (_, amount) ->
            val sweepAngle = ((amount / total) * 360f).toFloat() * animation.value
            val color = getChartColor(index)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun TrendChartSection(allTransactions: List<Transaction>, period: String, type: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Trends ($period)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            val chartData = prepareTrendData(allTransactions, period, type)

            if (chartData.isNotEmpty() && chartData.any { it.second > 0 }) {
                CustomBarChart(
                    data = chartData,
                    color = if(type == "income") Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data for trends", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CustomBarChart(
    data: List<Pair<String, Double>>,
    color: Color,
    modifier: Modifier
) {
    val maxValue = data.maxOfOrNull { it.second } ?: 1.0

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 1.5f)
        val space = (size.width - (barWidth * data.size)) / (data.size + 1)
        val height = size.height

        data.forEachIndexed { index, (label, value) ->
            val barHeight = (value / maxValue * (height - 40f)).toFloat().coerceAtLeast(4f)
            val x = space + (index * (barWidth + space))
            val y = height - barHeight - 40f // Leave space for text

            // Draw Bar
            drawRoundRect(
                color = if(value > 0) color else Color.LightGray.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Draw Label
            val textPaint = android.graphics.Paint().apply {
                setColor(android.graphics.Color.GRAY)
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2,
                height - 10f,
                textPaint
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun CategoryBreakdownItem(
    name: String,
    amount: Double,
    percentage: Float,
    type: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Percentage Bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if(type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if(type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                // Visual Progress Bar
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if(type == "income") Color(0xFF4CAF50) else Color(0xFFF44336),
                    trackColor = BackgroundGray,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = NumberFormat.getCurrencyInstance().format(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun EmptyChartState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No data for this period", color = TextGray)
        }
    }
}

// --- Helpers ---

// Custom helper to replace getCompactNumberInstance
fun formatCompactNumber(number: Double): String {
    if (number < 1000) return String.format("%.0f", number)
    val exp = (ln(number) / ln(1000.0)).toInt()
    val char = "KMGTPE"[exp - 1]
    return String.format("%.1f%c", number / 1000.0.pow(exp.toDouble()), char)
}

fun filterTransactions(transactions: List<Transaction>, period: String, type: String): List<Transaction> {
    val now = Calendar.getInstance()
    val filteredByType = transactions.filter { it.type == type }

    return when (period) {
        "Week" -> {
            now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
            val startOfWeek = now.time
            filteredByType.filter { it.date.toDate() >= startOfWeek }
        }
        "Month" -> {
            val currentMonth = now.get(Calendar.MONTH) + 1
            val currentYear = now.get(Calendar.YEAR)
            filteredByType.filter {
                // Fallback to parsing timestamp if month/year fields are 0 (legacy data)
                val txYear = if (it.year == 0) {
                    val c = Calendar.getInstance().apply { time = it.date.toDate() }
                    c.get(Calendar.YEAR)
                } else it.year

                val txMonth = if (it.month == 0) {
                    val c = Calendar.getInstance().apply { time = it.date.toDate() }
                    c.get(Calendar.MONTH) + 1
                } else it.month

                txMonth == currentMonth && txYear == currentYear
            }
        }
        "Year" -> {
            val currentYear = now.get(Calendar.YEAR)
            filteredByType.filter {
                val txYear = if (it.year == 0) {
                    val c = Calendar.getInstance().apply { time = it.date.toDate() }
                    c.get(Calendar.YEAR)
                } else it.year
                txYear == currentYear
            }
        }
        else -> filteredByType
    }
}

fun prepareTrendData(transactions: List<Transaction>, period: String, type: String): List<Pair<String, Double>> {
    val filtered = transactions.filter { it.type == type }
    val calendar = Calendar.getInstance()

    return when(period) {
        "Week" -> {
            (6 downTo 0).map { offset ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -offset)
                val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)

                val sum = filtered.filter {
                    val txCal = Calendar.getInstance().apply { time = it.date.toDate() }
                    txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                            txCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                }.sumOf { it.amount }
                dayOfWeek to sum
            }
        }
        "Month" -> {
            // Show 4 weeks breakdown for the current month
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            (1..4).map { week ->
                val sum = filtered.filter {
                    // Calculate month/year from date if missing
                    val c = Calendar.getInstance().apply { time = it.date.toDate() }
                    val txYear = if (it.year == 0) c.get(Calendar.YEAR) else it.year
                    val txMonth = if (it.month == 0) c.get(Calendar.MONTH) + 1 else it.month

                    txYear == currentYear && txMonth == currentMonth &&
                            // Week calculation
                            (c.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1 == week
                }.sumOf { it.amount }
                "W$week" to sum
            }
        }
        "Year" -> {
            val currentYear = calendar.get(Calendar.YEAR)
            (0..11).map { monthIndex ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1) // Avoid rollover bug
                cal.set(Calendar.MONTH, monthIndex)
                val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)

                val sum = filtered.filter {
                    val c = Calendar.getInstance().apply { time = it.date.toDate() }
                    val txYear = if (it.year == 0) c.get(Calendar.YEAR) else it.year
                    val txMonth = if (it.month == 0) c.get(Calendar.MONTH) + 1 else it.month

                    txYear == currentYear && txMonth == (monthIndex + 1)
                }.sumOf { it.amount }
                monthName to sum
            }
        }
        else -> emptyList()
    }
}

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF00796B), // MediumGreen
        Color(0xFF00A79B), // LightGreen
        Color(0xFFFFC107), // Amber
        Color(0xFFEF5350), // Red
        Color(0xFF42A5F5), // Blue
        Color(0xFF9C27B0), // Purple
        Color(0xFF795548)  // Brown
    )
    return colors[index % colors.size]
}