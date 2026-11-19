package com.firstlimo.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    initialType: TransactionType = TransactionType.COLLECTION,
    editingTransaction: FinancialTransaction? = null,
    onDismiss: () -> Unit,
    onSave: (type: TransactionType, amount: Double, notes: String, date: String, time: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.toString() ?: "") }
    var notesText by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    var dateText by remember { mutableStateOf(editingTransaction?.dateString ?: DateUtils.getCurrentDate()) }
    var timeText by remember { mutableStateOf(editingTransaction?.timeString ?: DateUtils.getCurrentTime()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ترويسة النافذة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) "تعديل العملية" else "تسجيل عملية جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // اختيار نوع العملية
            Text(
                text = "نوع العملية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeChip(
                        title = "تحصيل ليموزين",
                        isSelected = selectedType == TransactionType.COLLECTION,
                        color = EmeraldCollection,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.COLLECTION }

                    TypeChip(
                        title = "مصروف ليموزين",
                        isSelected = selectedType == TransactionType.EXPENSE,
                        color = CrimsonExpense,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.EXPENSE }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeChip(
                        title = "كوميسيون (سائق)",
                        isSelected = selectedType == TransactionType.COMMISSION,
                        color = PurpleCommission,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.COMMISSION }

                    TypeChip(
                        title = "إيداع نقدي شخصي",
                        isSelected = selectedType == TransactionType.PERSONAL_DEPOSIT,
                        color = AmberPersonalCash,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.PERSONAL_DEPOSIT }
                }

                TypeChip(
                    title = "مصروف نقدي شخصي",
                    isSelected = selectedType == TransactionType.PERSONAL_EXPENSE,
                    color = CrimsonExpense,
                    modifier = Modifier.fillMaxWidth()
                ) { selectedType = TransactionType.PERSONAL_EXPENSE }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // إدخال المبلغ
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("المبلغ (ج.م)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = CardBorder
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = CrimsonExpense,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // التاريخ والوقت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("التاريخ") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = CardBorder
                    )
                )

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("الوقت") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = CardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // الملاحظات والتفاصيل
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("التفاصيل أو الملاحظات (اختياري)") },
                placeholder = { Text("مثال: بنزين، غسيل، رقم الرحلة...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = CardBorder
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // زر الحفظ
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    onSave(selectedType, amount, notesText.trim(), dateText.trim(), timeText.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
            ) {
                Text(
                    text = if (editingTransaction != null) "تحديث العملية" else "حفظ العملية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TypeChip(
    title: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else TextPrimary
            )
        }
    }
}
