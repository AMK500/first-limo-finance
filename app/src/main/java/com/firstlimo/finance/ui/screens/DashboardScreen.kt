package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.components.*
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val summary by viewModel.financialSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedSheetType by remember { mutableStateOf(TransactionType.COLLECTION) }
    var editingTransaction by remember { mutableStateOf<FinancialTransaction?>(null) }
    var showTripCalculator by remember { mutableStateOf(false) }

    // فحص التحديثات بهدوء عند بدء الشاشة
    LaunchedEffect(Unit) {
        viewModel.checkForAppUpdates(isManualCheck = false)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "فرست ليموزين",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "الإدارة المالية وحساب النقدية",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    SyncStatusBadge(
                        isOnline = isOnline,
                        isSyncing = isSyncing,
                        onManualSync = { viewModel.syncUnsynced() }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "سجل العمليات",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "الرسوم البيانية",
                            tint = AccentGold
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. بطاقة صافي حساب فرست ليموزين (الحساب النهائي للشركة)
            item {
                BalanceCard(
                    title = "صافي حساب فرست ليموزين",
                    subtitle = "التحصيل − المصاريف − الكوميسيون",
                    amount = summary.netLimoBalance,
                    cardColor = BlueLimoNet,
                    icon = Icons.Default.DirectionsCar,
                    isLarge = true
                )
            }

            // 2. إجمالي النقدية الشخصية (أموال السائق المستقلة)
            item {
                BalanceCard(
                    title = "إجمالي النقدية الشخصية",
                    subtitle = "الكوميسيون + الإيداعات − المصاريف الشخصية",
                    amount = summary.totalPersonalCash,
                    cardColor = AmberPersonalCash,
                    icon = Icons.Default.AccountBalanceWallet,
                    isLarge = true
                )
            }

            // 3. إجمالي السيولة الإجمالية (الحسابين معاً)
            item {
                BalanceCard(
                    title = "إجمالي السيولة (الحسابين معاً)",
                    subtitle = "إجمالي النقدية المتاحة حالياً",
                    amount = summary.totalOverallLiquidity,
                    cardColor = IndigoTotalLiquidity,
                    icon = Icons.Default.Savings,
                    isLarge = false
                )
            }

            // 4. شبكة البطاقات الثلاثة (التحصيل، المصاريف، الكوميسيون)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BalanceMiniCard(
                        title = "التحصيل",
                        amount = summary.totalCollection,
                        color = EmeraldCollection,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                    BalanceMiniCard(
                        title = "المصاريف",
                        amount = summary.totalExpense,
                        color = CrimsonExpense,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    BalanceMiniCard(
                        title = "الكوميسيون",
                        amount = summary.totalCommission,
                        color = PurpleCommission,
                        icon = Icons.Default.Stars,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. قسم أزرار الإجراء السريع (Quick Actions)
            item {
                Text(
                    text = "تسجيل عملية سريعة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "+ تحصيل",
                        color = EmeraldCollection,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedSheetType = TransactionType.COLLECTION
                        editingTransaction = null
                        showAddSheet = true
                    }
                    QuickActionButton(
                        title = "- مصروف",
                        color = CrimsonExpense,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedSheetType = TransactionType.EXPENSE
                        editingTransaction = null
                        showAddSheet = true
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "+ كوميسيون",
                        color = PurpleCommission,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedSheetType = TransactionType.COMMISSION
                        editingTransaction = null
                        showAddSheet = true
                    }
                    QuickActionButton(
                        title = "حركة نقدية",
                        color = AmberPersonalCash,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedSheetType = TransactionType.PERSONAL_DEPOSIT
                        editingTransaction = null
                        showAddSheet = true
                    }
                }
            }

            // أداة تسعير المشاوير والبنزين
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTripCalculator = true },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("حاسبة تسعير المشاوير والبنزين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("حساب فوري لتكلفة الكيلومتر والرسوم وصافي الربح", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 6. آخر العمليات
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر العمليات المسجلة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "عرض الكل (${transactions.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }

            val recentTransactions = transactions.take(5)
            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات مسجلة حتى الآن.\nاضغط على أي زر أعلاه لإضافة عملية.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onEdit = {
                            editingTransaction = it
                            selectedSheetType = it.type
                            showAddSheet = true
                        },
                        onDelete = { viewModel.deleteTransaction(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // نافذة إضافة/تعديل عملية
    if (showAddSheet) {
        AddTransactionSheet(
            initialType = selectedSheetType,
            editingTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSave = { type, amount, notes, date, time, receiptPath ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            type = type,
                            amount = amount,
                            notes = notes,
                            dateString = date,
                            timeString = time,
                            receiptImagePath = receiptPath
                        )
                    )
                } else {
                    viewModel.addTransaction(type, amount, notes, date, time, receiptPath)
                }
                showAddSheet = false
                editingTransaction = null
            }
        )
    }

    // حوار التحديث التلقائي في حال توفره
    if (availableUpdate != null) {
        UpdateAvailableDialog(
            release = availableUpdate!!,
            downloadProgress = downloadProgress,
            onUpdateClick = { viewModel.startInAppUpdate() },
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    // حاسبة تسعير المشاوير
    if (showTripCalculator) {
        TripPriceCalculatorDialog(
            onDismiss = { showTripCalculator = false }
        )
    }
}

@Composable
fun BalanceMiniCard(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${java.text.DecimalFormat("#,##0").format(amount)} ج.م",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
