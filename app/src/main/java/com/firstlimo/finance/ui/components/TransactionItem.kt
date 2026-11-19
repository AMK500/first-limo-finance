package com.firstlimo.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.theme.*
import java.text.DecimalFormat

@Composable
fun TransactionItem(
    transaction: FinancialTransaction,
    onEdit: (FinancialTransaction) -> Unit,
    onDelete: (FinancialTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val (badgeColor, signPrefix) = when (transaction.type) {
        TransactionType.COLLECTION -> Pair(EmeraldCollection, "+")
        TransactionType.EXPENSE -> Pair(CrimsonExpense, "-")
        TransactionType.COMMISSION -> Pair(PurpleCommission, "+")
        TransactionType.PERSONAL_DEPOSIT -> Pair(AmberPersonalCash, "+")
        TransactionType.PERSONAL_EXPENSE -> Pair(CrimsonExpense, "-")
    }

    val formatter = DecimalFormat("#,##0.00")
    val formattedAmount = "$signPrefix${formatter.format(transaction.amount)} ج.م"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                // شريط/مؤشر ملون لنوع العملية
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.type.titleArabic,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    if (transaction.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.notes,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${transaction.dateString} • ${transaction.timeString}",
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedAmount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit(transaction)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف", color = CrimsonExpense) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonExpense) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من حذف هذه العملية؟ سيتم حذفها ومزامنتها عبر جميع الأجهزة.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(transaction)
                    }
                ) {
                    Text("حذف", color = CrimsonExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
