package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.components.AddTransactionSheet
import com.firstlimo.finance.ui.components.TransactionItem
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel
import com.firstlimo.finance.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsHistoryScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var editingTransaction by remember { mutableStateOf<FinancialTransaction?>(null) }

    // تصفية العمليات حسب البحث والنوع
    val filteredTransactions = remember(transactions, searchQuery, selectedFilterType) {
        transactions.filter { tx ->
            val matchesType = selectedFilterType == null || tx.type == selectedFilterType
            val matchesSearch = searchQuery.isBlank() ||
                    tx.notes.contains(searchQuery, ignoreCase = true) ||
                    tx.amount.toString().contains(searchQuery) ||
                    tx.dateString.contains(searchQuery)
            matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "سجل العمليات (${filteredTransactions.size})",
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
                actions = {
                    IconButton(
                        onClick = {
                            val file = ExportUtils.exportToCsv(context, filteredTransactions, summary)
                            if (file != null) {
                                ExportUtils.shareFile(context, file)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "تصدير كشف الحساب",
                            tint = AccentBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في الملاحظات، المبالغ، التواريخ...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCardSurface,
                    unfocusedContainerColor = DarkCardSurface
                )
            )

            // شريط الفلاتر السريع
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterType == null,
                    onClick = { selectedFilterType = null },
                    label = { Text("الكل") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AccentBlue,
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.COLLECTION,
                    onClick = { selectedFilterType = TransactionType.COLLECTION },
                    label = { Text("تحصيل ليموزين") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldCollection.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldCollection,
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.EXPENSE,
                    onClick = { selectedFilterType = TransactionType.EXPENSE },
                    label = { Text("مصاريف ليموزين") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonExpense.copy(alpha = 0.2f),
                        selectedLabelColor = CrimsonExpense,
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.COMMISSION,
                    onClick = { selectedFilterType = TransactionType.COMMISSION },
                    label = { Text("الكوميسيون") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleCommission.copy(alpha = 0.2f),
                        selectedLabelColor = PurpleCommission,
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.PERSONAL_DEPOSIT,
                    onClick = { selectedFilterType = TransactionType.PERSONAL_DEPOSIT },
                    label = { Text("إيداع شخصي") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPersonalCash.copy(alpha = 0.2f),
                        selectedLabelColor = AmberPersonalCash,
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.PERSONAL_EXPENSE,
                    onClick = { selectedFilterType = TransactionType.PERSONAL_EXPENSE },
                    label = { Text("مصروف شخصي") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonExpense.copy(alpha = 0.2f),
                        selectedLabelColor = CrimsonExpense,
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary
                    )
                )
            }

            // قائمة السجل
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد نتائج مطابقة لبحثك أو فلترك.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onEdit = { editingTransaction = it },
                            onDelete = { viewModel.deleteTransaction(it) }
                        )
                    }
                }
            }
        }
    }

    if (editingTransaction != null) {
        AddTransactionSheet(
            initialType = editingTransaction!!.type,
            editingTransaction = editingTransaction,
            onDismiss = { editingTransaction = null },
            onSave = { type, amount, notes, date, time, receiptPath ->
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
                editingTransaction = null
            }
        )
    }
}
