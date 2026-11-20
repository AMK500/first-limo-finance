package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    val formatter = DecimalFormat("#,##0")

    // تحليل بنود المصاريف من خلال الملاحظات
    val expenseBreakdown = remember(transactions) {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE && !it.isDeleted }
        var fuel = 0.0
        var toll = 0.0
        var wash = 0.0
        var maintenance = 0.0
        var other = 0.0

        for (tx in expenses) {
            val notes = tx.notes.lowercase()
            when {
                notes.contains("بنزين") || notes.contains("وقود") || notes.contains("سولار") -> fuel += tx.amount
                notes.contains("كارتة") || notes.contains("بوابة") || notes.contains("طريق") -> toll += tx.amount
                notes.contains("غسيل") || notes.contains("نظافة") -> wash += tx.amount
                notes.contains("صيانة") || notes.contains("زيت") || notes.contains("كاوتش") || notes.contains("تيل") -> maintenance += tx.amount
                else -> other += tx.amount
            }
        }
        listOf(
            Triple("بنزين ووقود", fuel, Color(0xFFF59E0B)),
            Triple("كارتات وبوابات", toll, Color(0xFF38BDF8)),
            Triple("غسيل ونظافة", wash, Color(0xFF10B981)),
            Triple("صيانة وقطع غيار", maintenance, Color(0xFFEF4444)),
            Triple("مصاريف أخرى", other, Color(0xFFA855F7))
        ).filter { it.second > 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الرسوم البيانية والإحصائيات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // كارت المقارنة البصرية للتدفقات المالية
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مقارنة التدفقات المالية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val maxAmount = maxOf(summary.totalCollection, summary.totalExpense, summary.totalCommission, 1.0)

                        // شريط التحصيل
                        ChartBarRow(
                            label = "إجمالي التحصيل",
                            amount = summary.totalCollection,
                            progress = (summary.totalCollection / maxAmount).toFloat(),
                            color = EmeraldCollection
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // شريط المصاريف
                        ChartBarRow(
                            label = "إجمالي المصاريف",
                            amount = summary.totalExpense,
                            progress = (summary.totalExpense / maxAmount).toFloat(),
                            color = CrimsonExpense
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // شريط الكوميسيون
                        ChartBarRow(
                            label = "إجمالي الكوميسيون",
                            amount = summary.totalCommission,
                            progress = (summary.totalCommission / maxAmount).toFloat(),
                            color = PurpleCommission
                        )
                    }
                }
            }

            // كارت النسب المئوية وتوزيع الإيرادات
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = AccentGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "توزيع إيرادات التحصيل",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val totalCol = if (summary.totalCollection > 0) summary.totalCollection else 1.0
                        val expensePercent = ((summary.totalExpense / totalCol) * 100).toInt().coerceIn(0, 100)
                        val commissionPercent = ((summary.totalCommission / totalCol) * 100).toInt().coerceIn(0, 100)
                        val netLimoPercent = (((summary.netLimoBalance) / totalCol) * 100).toInt().coerceIn(0, 100)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PercentBox("المصاريف", "$expensePercent%", CrimsonExpense, Modifier.weight(1f))
                            PercentBox("الكوميسيون", "$commissionPercent%", PurpleCommission, Modifier.weight(1f))
                            PercentBox("صافي الشركة", "$netLimoPercent%", BlueLimoNet, Modifier.weight(1f))
                        }
                    }
                }
            }

            // كارت تفصيل أوجه الصرف (بنزين، كارتات، صيانة)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "تفصيل مصاريف السيارة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (expenseBreakdown.isEmpty()) {
                            Text(
                                text = "لا توجد مصاريف مسجلة حتى الآن لتحليلها.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val totalExp = if (summary.totalExpense > 0) summary.totalExpense else 1.0
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                for ((category, amount, color) in expenseBreakdown) {
                                    val percent = ((amount / totalExp) * 100).toInt()
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "$category ($percent%)", fontSize = 13.sp, color = TextPrimary)
                                            Text(text = "${formatter.format(amount)} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (amount / totalExp).toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = color,
                                            trackColor = DarkCardBorder
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ChartBarRow(
    label: String,
    amount: Double,
    progress: Float,
    color: Color
) {
    val formatter = DecimalFormat("#,##0")
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = TextSecondary)
            Text(text = "${formatter.format(amount)} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0.02f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = DarkCardBorder
        )
    }
}

@Composable
fun PercentBox(
    label: String,
    percent: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = percent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
