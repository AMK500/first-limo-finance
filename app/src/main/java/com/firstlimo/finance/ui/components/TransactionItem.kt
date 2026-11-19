package com.firstlimo.finance.ui.components

import androidx.compose.foundation.BorderStroke
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        fontWeight = FontWeight.Bold,
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
                        color = TextSecondary.copy(alpha = 0.7f)
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
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentBlue) },
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
            containerColor = DarkSurface,
            title = { Text("تأكيد الحذف", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذه العملية؟ سيتم تحديث الأرصدة فوراً.", color = TextSecondary) },
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
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}
