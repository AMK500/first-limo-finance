package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

    // فحص التحديثات عند بدء الشاشة
    LaunchedEffect(Unit) {
        viewModel.checkForAppUpdates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "فرست ليموزين",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "الإدارة المالية وحساب النقدية",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
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
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryDark
                )
            )
        },
        containerColor = BackgroundLight
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
                    subtitle = "إجمالي الأموال المتاحة حالياً",
                    amount = summary.totalOverallLiquidity,
                    cardColor = NavyTotalLiquidity,
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
            onSave = { type, amount, notes, date, time ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            type = type,
                            amount = amount,
                            notes = notes,
                            dateString = date,
                            timeString = time
                        )
                    )
                } else {
                    viewModel.addTransaction(type, amount, notes, date, time)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${java.text.DecimalFormat("#,##0").format(amount)} ج.م",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
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
        shape = RoundedCornerShape(12.dp),
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
