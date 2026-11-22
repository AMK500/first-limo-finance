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
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
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
                        onClick = { onCustomerClick(item.customer) },
                        onTogglePin = { viewModel.togglePinCustomer(item.customer) },
                        onEdit = if (!item.customer.isSystem) { { customerToEdit = item.customer } } else null,
                        onDelete = if (!item.customer.isSystem) { { customerToDelete = item.customer } } else null
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

    // نافذة تعديل بيانات العميل
    if (customerToEdit != null) {
        AddCustomerDialog(
            initialCustomer = customerToEdit,
            onDismiss = { customerToEdit = null },
            onConfirm = { name, phone, address, notes ->
                viewModel.updateCustomer(customerToEdit!!, name, phone, address, notes)
                customerToEdit = null
            }
        )
    }

    // نافذة تأكيد حذف العميل
    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("حذف العميل", color = CrimsonExpense, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text("هل أنت متأكد من حذف العميل \"${customerToDelete!!.name}\" وجميع معاملاته نهائياً؟", color = TextPrimary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customerToDelete!!)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkCardSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun CustomerCardItem(
    item: CustomerWithBalance,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val customer = item.customer
    val netBalance = item.netBalance
    val formatter = DecimalFormat("#,##0.00")

    val isPositive = netBalance > 0
    val isNegative = netBalance < 0
    val balanceColor = if (isPositive) EmeraldCollection else if (isNegative) CrimsonExpense else TextSecondary

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = BorderStroke(
            if (customer.isPinned) 1.5.dp else if (customer.isSystem) 1.5.dp else 1.dp,
            if (customer.isPinned) AccentGold.copy(alpha = 0.7f) else if (customer.isSystem) AccentGold.copy(alpha = 0.5f) else DarkCardBorder
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
                            if (customer.isPinned) AccentGold.copy(alpha = 0.25f) else if (customer.isSystem) AccentGold.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.15f),
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
                            color = if (customer.isPinned) AccentGold else AccentBlue
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
                        if (customer.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentGold.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "مثبت",
                                        tint = AccentGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "مثبت",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                }
                            }
                        }
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

            // رصيد العميل وقائمة الخيارات (الثلاث نقاط) على اليسار
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatter.format(Math.abs(netBalance))} ج.م",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                    Text(
                        text = if (isPositive) "أخذت (لك)" else if (isNegative) "أعطيت (عليك)" else "خالص",
                        fontSize = 11.sp,
                        color = balanceColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // قائمة الثلاث نقاط ⋮
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات العميل",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkCardSurface)
                    ) {
                        // 1. تعديل بيانات العميل
                        if (!customer.isSystem && onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("تعديل العميل", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                        }

                        // 2. تثبيت العميل في الأعلى / إلغاء التثبيت
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (customer.isPinned) "إلغاء التثبيت" else "تثبيت في الأعلى",
                                    color = if (customer.isPinned) AccentGold else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = if (customer.isPinned) AccentGold else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            }
                        )

                        // 3. حذف العميل
                        if (!customer.isSystem && onDelete != null) {
                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("حذف العميل", color = CrimsonExpense, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = CrimsonExpense,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
