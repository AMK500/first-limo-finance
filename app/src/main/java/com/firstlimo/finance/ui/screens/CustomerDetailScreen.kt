package com.firstlimo.finance.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.data.local.DebtTransactionEntity
import com.firstlimo.finance.data.local.DebtType
import com.firstlimo.finance.ui.components.AddDebtTransactionSheet
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.DebtsViewModel
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity,
    viewModel: DebtsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.selectedCustomerTransactions.collectAsState()
    val allCustomersWithBalance by viewModel.customersWithBalance.collectAsState()
    val currentCustomer = allCustomersWithBalance.firstOrNull { it.customer.id == customer.id }?.customer ?: customer

    var showAddSheet by remember { mutableStateOf(false) }
    var sheetType by remember { mutableStateOf(DebtType.TOOK) }
    var editingTransaction by remember { mutableStateOf<DebtTransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<DebtTransactionEntity?>(null) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("الكل") }

    val todayStr = com.firstlimo.finance.utils.DateUtils.getCurrentDate()
    val filteredTransactions = remember(transactions, selectedPeriod) {
        when (selectedPeriod) {
            "اليوم" -> transactions.filter { it.dateString == todayStr }
            "هذا الأسبوع" -> transactions.filter { com.firstlimo.finance.utils.DateUtils.isWithinDays(it.dateString, 7) }
            "هذا الشهر" -> {
                val prefix = todayStr.substringBeforeLast("-")
                transactions.filter { it.dateString.startsWith(prefix) }
            }
            else -> transactions
        }
    }

    val totalTook = filteredTransactions.filter { it.type == DebtType.TOOK.name }.sumOf { it.amount }
    val totalGave = filteredTransactions.filter { it.type == DebtType.GAVE.name }.sumOf { it.amount }
    val netBalance = totalTook - totalGave

    val formatter = DecimalFormat("#,##0.00")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (customer.isSystem) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AccentGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "مربوط بالمالية",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (!customer.phone.isNullOrBlank()) {
                            Text(
                                text = customer.phone,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                actions = {
                    // زر التثبيت في الأعلى 📌
                    IconButton(onClick = { viewModel.togglePinCustomer(currentCustomer) }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (currentCustomer.isPinned) "إلغاء التثبيت" else "تثبيت العميل في الأعلى",
                            tint = if (currentCustomer.isPinned) AccentGold else TextSecondary
                        )
                    }

                    // زر حذف العميل (متاح فقط للعملاء العاديين)
                    if (!customer.isSystem) {
                        IconButton(onClick = { showDeleteCustomerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف العميل",
                                tint = CrimsonExpense
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            // شريط الزرين السفليين العريضين (أخذت أخضر / أعطيت أحمر) مع مسافة أمان لتفادي أزرار الهاتف
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // مسافة أمان شاملة
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            editingTransaction = null
                            sheetType = DebtType.GAVE
                            showAddSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("أعطيت", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            editingTransaction = null
                            sheetType = DebtType.TOOK
                            showAddSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollection)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("أخذت", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // كارت الرصيد الحالي للعميل
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                if (netBalance >= 0) EmeraldCollection.copy(alpha = 0.6f) else CrimsonExpense.copy(alpha = 0.6f),
                                DarkCardBorder
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (netBalance > 0) "صافي الرصيد (مستحق لك)" else if (netBalance < 0) "صافي الرصيد (مطلوب منك)" else "الرصيد خالص",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${formatter.format(Math.abs(netBalance))} ج.م",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netBalance > 0) EmeraldCollection else if (netBalance < 0) CrimsonExpense else AccentGold
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DarkCardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي ما أخذته", fontSize = 11.sp, color = TextSecondary)
                                Text("${formatter.format(totalTook)} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldCollection)
                            }
                            Box(modifier = Modifier.height(28.dp).width(1.dp).background(DarkCardBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي ما أعطيته", fontSize = 11.sp, color = TextSecondary)
                                Text("${formatter.format(totalGave)} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CrimsonExpense)
                            }
                        }
                    }
                }
            }

            // أزرار الإجراءات السريعة (اتصال، مشاركة، ملاحظات)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!customer.phone.isNullOrBlank()) {
                        QuickActionButton(
                            icon = Icons.Default.Call,
                            label = "اتصال",
                            modifier = Modifier.weight(1f)
                        ) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                            context.startActivity(intent)
                        }
                    }

                    QuickActionButton(
                        icon = Icons.Default.Share,
                        label = "مشاركة الحساب",
                        modifier = Modifier.weight(1f)
                    ) {
                        val shareText = "كشف حساب: ${customer.name}\nالرصيد الحالي: ${formatter.format(netBalance)} ج.م\nتم الاستخراج عبر تطبيق فرست ليموزين."
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة كشف الحساب"))
                    }

                    if (!customer.notes.isNullOrBlank()) {
                        QuickActionButton(
                            icon = Icons.Default.Note,
                            label = "ملاحظة",
                            modifier = Modifier.weight(1f)
                        ) {
                            // إشعار أو استعراض
                        }
                    }
                }
            }

            // شريط فلاتر الفترة الزمنية
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("الكل", "اليوم", "هذا الأسبوع", "هذا الشهر").forEach { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AccentBlue,
                                containerColor = DarkCardSurface,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) AccentBlue else DarkCardBorder)
                        )
                    }
                }
            }

            // ترويسة سجل المعاملات
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل المعاملات (${filteredTransactions.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // قائمة المعاملات
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لا توجد معاملات مسجلة في هذه الفترة", color = TextSecondary, fontSize = 14.sp)
                            Text("استخدم زري 'أخذت' أو 'أعطيت' بالأسفل لبدء التسجيل", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val isTook = tx.type == DebtType.TOOK.name
                    val itemColor = if (isTook) EmeraldCollection else CrimsonExpense

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingTransaction = tx
                                sheetType = if (isTook) DebtType.TOOK else DebtType.GAVE
                                showAddSheet = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(itemColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isTook) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = itemColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isTook) "أخذت" else "أعطيت",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = itemColor
                                        )
                                        if (!tx.receiptImagePath.isNullOrBlank() && File(tx.receiptImagePath).exists()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Image, contentDescription = "مرفق فاتورة", tint = AccentBlue, modifier = Modifier.size(14.dp))
                                        }
                                        if (!tx.linkedFinancialTransactionId.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Sync, contentDescription = "مربوط بالمالية", tint = AccentGold, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (tx.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = tx.notes, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${tx.dateString} • ${tx.timeString}",
                                        fontSize = 11.sp,
                                        color = TextTertiary
                                    )

                                    // تنبيه موعد الاستحقاق (سلفيات / جمعيات)
                                    if (!tx.dueDateString.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val isPast = com.firstlimo.finance.utils.DateUtils.isPastDate(tx.dueDateString)
                                        val isToday = com.firstlimo.finance.utils.DateUtils.isToday(tx.dueDateString)
                                        val badgeColor = if (isPast) CrimsonExpense else if (isToday) AccentGold else AccentBlue
                                        val badgeText = if (isPast) "مستحق السداد (${tx.dueDateString})" else if (isToday) "يستحق اليوم (${tx.dueDateString})" else "موعد السداد: ${tx.dueDateString}"
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${if (isTook) "+" else "-"}${formatter.format(tx.amount)} ج.م",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = itemColor
                                )
                                IconButton(onClick = { transactionToDelete = tx }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CrimsonExpense.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // تأكيد الحذف
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = DarkSurface,
            title = { Text("تأكيد الحذف", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذه المعاملة؟", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteDebtTransaction(it) }
                        transactionToDelete = null
                    }
                ) {
                    Text("حذف", color = CrimsonExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }

    // نافذة تسجيل المعاملة بالحاسبة
    if (showAddSheet) {
        AddDebtTransactionSheet(
            customerName = customer.name,
            initialType = sheetType,
            editingTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSave = { type, amount, notes, date, time, receiptPath, dueDate ->
                viewModel.addDebtTransaction(
                    customerId = customer.id,
                    isSystemCustomer = customer.isSystem,
                    type = type.name,
                    amount = amount,
                    notes = notes,
                    date = date,
                    time = time,
                    receiptPath = receiptPath,
                    editingId = editingTransaction?.id,
                    dueDate = dueDate
                )
                showAddSheet = false
                editingTransaction = null
            }
        )
    }

    // نافذة تأكيد حذف العميل
    if (showDeleteCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerDialog = false },
            title = {
                Text(
                    text = "حذف العميل",
                    color = CrimsonExpense,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف العميل \"${customer.name}\" وكافة المعاملات المالية المربوطة به نهائياً؟",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        showDeleteCustomerDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkCardSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = DarkCardSurface,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
