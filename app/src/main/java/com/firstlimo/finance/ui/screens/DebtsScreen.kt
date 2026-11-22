package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.data.model.CustomerWithBalance
import com.firstlimo.finance.ui.components.AddCustomerDialog
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.DebtsViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtsViewModel,
    onCustomerClick: (CustomerEntity) -> Unit
) {
    val customers by viewModel.customersWithBalance.collectAsState()
    val summary by viewModel.debtsSummary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    val formatter = DecimalFormat("#,##0.00")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = DarkBackground,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("إضافة عميل", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
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
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. كارت ملخص الديون الإجمالي (أخذت أخضر / أعطيت أحمر)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ملخص دفتر الديون",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface
                            ) {
                                Text(
                                    text = "العملاء (${customers.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // أخذت (لي عند العملاء)
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(text = "أخذت (قبضت)", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatter.format(summary.totalTookAll)} ج.م",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldCollection
                                )
                            }

                            // أعطيت (عليّ للعملاء)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "أعطيت (دفعت)", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatter.format(summary.totalGaveAll)} ج.م",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonExpense
                                )
                            }
                        }
                    }
                }
            }

            // 2. شريط البحث الفوري
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("بحث عن عميل بالاسم أو رقم الهاتف...", color = TextTertiary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkCardSurface,
                        unfocusedContainerColor = DarkCardSurface
                    )
                )
            }

            // 3. ترويسة قائمة العملاء
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة الحسابات والعملاء",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            // 4. قائمة العملاء
            if (customers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لا يوجد عملاء يطابقون البحث", color = TextSecondary, fontSize = 14.sp)
                            Text("اضغط على زر '+ إضافة عميل' لإضافة عميلك الأول", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(customers, key = { it.customer.id }) { item ->
                    CustomerCardItem(
                        item = item,
                        onClick = { onCustomerClick(item.customer) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }

    // نافذة إضافة عميل جديد
    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, address, notes ->
                viewModel.addCustomer(name, phone, address, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CustomerCardItem(
    item: CustomerWithBalance,
    onClick: () -> Unit
) {
    val customer = item.customer
    val netBalance = item.netBalance
    val formatter = DecimalFormat("#,##0.00")

    val isPositive = netBalance > 0
    val isNegative = netBalance < 0
    val balanceColor = if (isPositive) EmeraldCollection else if (isNegative) CrimsonExpense else TextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = BorderStroke(
            if (customer.isSystem) 1.5.dp else 1.dp,
            if (customer.isSystem) AccentGold.copy(alpha = 0.5f) else DarkCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // الحرف الأول أو أيقونة الشركة
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (customer.isSystem) AccentGold.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (customer.isSystem) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = customer.name.firstOrNull()?.toString() ?: "ع",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = customer.name,
                            fontSize = 15.sp,
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
                                    text = "حساب الشركة",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!customer.phone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = customer.phone,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (!item.lastTransactionDate.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "آخر حركة: ${item.lastTransactionDate}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }

            // رصيد العميل على اليسار
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatter.format(Math.abs(netBalance))} ج.م",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
                Text(
                    text = if (isPositive) "أخذت (لك)" else if (isNegative) "أعطيت (عليك)" else "خالص",
                    fontSize = 11.sp,
                    color = balanceColor
                )
            }
        }
    }
}
